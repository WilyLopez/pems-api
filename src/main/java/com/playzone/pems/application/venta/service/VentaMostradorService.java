package com.playzone.pems.application.venta.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.venta.dto.command.NinoMostradorCommand;
import com.playzone.pems.application.venta.dto.command.PagoMostradorCommand;
import com.playzone.pems.application.venta.dto.command.RegistrarVentaMostradorCommand;
import com.playzone.pems.application.venta.dto.query.VentaMostradorQuery;
import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.domain.promocion.model.Promocion;
import com.playzone.pems.domain.promocion.repository.PromocionRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.FechaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VentaMostradorService {

    private static final Long ID_CLIENTE_ANONIMO = 1L;

    private final VentaRepository          ventaRepository;
    private final ReservaPublicaRepository reservaRepository;
    private final VentaPagoRepository      ventaPagoRepository;
    private final SesionCajaRepository     sesionCajaRepository;
    private final EnrutadorCajaService     enrutadorCajaService;
    private final TarifaRepository         tarifaRepository;
    private final FeriadoRepository        feriadoRepository;
    private final PromocionRepository      promocionRepository;
    private final SupabaseAuthFacade       authFacade;
    private final ConfiguracionCalendarioRepository configRepository;
    private final RegistrarLogUseCase      auditoria;

    @Transactional
    public VentaMostradorQuery registrar(RegistrarVentaMostradorCommand cmd) {

        UUID usuarioActual = authFacade.usuarioActualId()
                .orElseThrow(() -> new ValidationException(
                        "Sesion requerida para registrar venta en mostrador."));
        if (sesionCajaRepository.findAbiertaByUsuarioAndSede(usuarioActual, cmd.getSedeId()).isEmpty()) {
            throw new ValidationException(
                    "No tienes una caja abierta en la sede indicada. Abre tu caja antes de registrar ventas.");
        }

        java.time.ZoneId zoneId = java.time.ZoneId.of("America/Lima");
        java.time.LocalTime horaActual = java.time.LocalTime.now(zoneId);
        java.time.LocalTime apertura = java.time.LocalTime.of(10, 0);
        java.time.LocalTime cierre = java.time.LocalTime.of(20, 0);

        try {
            var config = configRepository.obtener(cmd.getSedeId());
            if (config != null) {
                if (config.getHoraApertura() != null) apertura = config.getHoraApertura();
                if (config.getHoraCierre() != null) cierre = config.getHoraCierre();
            }
        } catch (Exception ignored) {}

        boolean esHoy = cmd.getFechaVisita().equals(LocalDate.now(zoneId));

        if (esHoy && horaActual.isAfter(cierre)) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
            throw new ValidationException(String.format("El local ya cerró por hoy. No se permiten registrar visitas para el día de hoy después de la hora de cierre: %s.",
                    cierre.format(formatter)));
        }

        boolean esFeriado = feriadoRepository.existsByFecha(cmd.getFechaVisita());
        TipoDia tipoDia = (FechaUtil.esFindeSemana(cmd.getFechaVisita()) || esFeriado)
                ? TipoDia.FIN_SEMANA_FERIADO
                : TipoDia.SEMANA;

        Tarifa tarifa = tarifaRepository
                .findVigenteBySedeAndTipoDiaAndFecha(cmd.getSedeId(), tipoDia, cmd.getFechaVisita())
                .orElseThrow(() -> new ValidationException(
                        "No existe tarifa vigente para la fecha indicada."));

        BigDecimal precioBase    = tarifa.getPrecio();
        int        cantidadNinos = cmd.getNinos().size();
        BigDecimal subtotal      = precioBase.multiply(BigDecimal.valueOf(cantidadNinos));

        BigDecimal descuento = BigDecimal.ZERO;
        if (cmd.getIdPromocion() != null) {
            Promocion promo = promocionRepository.findById(cmd.getIdPromocion())
                    .orElseThrow(() -> new ValidationException("Promocion no valida o expirada."));
            if (!promo.estaVigenteEn(cmd.getFechaVisita())) {
                throw new ValidationException("La promocion no esta vigente para esa fecha.");
            }
            if (!promo.cumpleMinimoPersonas(cantidadNinos)) {
                throw new ValidationException(
                        "La promocion requiere al menos " + promo.getMinimoPersonas() + " personas.");
            }
            descuento = promo.calcularDescuento(subtotal);
        }

        BigDecimal total = subtotal.subtract(descuento).max(BigDecimal.ZERO);

        BigDecimal efectivoRecibido = cmd.getEfectivoRecibido() != null ? cmd.getEfectivoRecibido() : BigDecimal.ZERO;
        BigDecimal vuelto = VentaPagoValidator.validarYCalcularVuelto(cmd.getPagos(), total, efectivoRecibido);

        Long clienteEfectivo = cmd.getClienteId() != null ? cmd.getClienteId() : ID_CLIENTE_ANONIMO;

        Venta ventaGuardada = ventaRepository.save(Venta.builder()
                .idSede(cmd.getSedeId())
                .clienteId(clienteEfectivo)
                .tipo("RESERVA")
                .canalCodigo("MOSTRADOR")
                .fechaVisita(cmd.getFechaVisita())
                .nombreAcompanante(cmd.getNombreAcompanante())
                .dniAcompanante(cmd.getDniAcompanante())
                .telefonoAcompanante(cmd.getTelefonoAcompanante())
                .promocionId(cmd.getIdPromocion())
                .subtotal(subtotal)
                .descuento(descuento)
                .total(total)
                .efectivoRecibido(efectivoRecibido)
                .vuelto(vuelto)
                .actaFirmada(cmd.isActaFirmada())
                .esAnticipada(cmd.getFechaVisita().isAfter(LocalDate.now()))
                .notas(cmd.getNotas())
                .createdBy(usuarioActual)
                .build());

        BigDecimal descuentoPorNino = cantidadNinos > 0
                ? descuento.divide(BigDecimal.valueOf(cantidadNinos), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal totalPorNino = precioBase.subtract(descuentoPorNino).max(BigDecimal.ZERO);

        EstadoReservaPublica estadoInicial = EstadoReservaPublica.CONFIRMADA;
        boolean ingresado = false;
        OffsetDateTime ingresoAt = null;

        if (esHoy) {
            if (horaActual.isBefore(apertura)) {
                estadoInicial = EstadoReservaPublica.CONFIRMADA;
            } else {
                estadoInicial = EstadoReservaPublica.COMPLETADA;
                ingresado = true;
                ingresoAt = OffsetDateTime.now(zoneId);
            }
        }

        List<ReservaPublica> reservas = new ArrayList<>();
        for (NinoMostradorCommand nino : cmd.getNinos()) {
            ReservaPublica reserva = reservaRepository.save(ReservaPublica.builder()
                    .ventaId(ventaGuardada.getId())
                    .idSede(cmd.getSedeId())
                    .idCliente(clienteEfectivo)
                    .canalReserva(CanalReserva.MOSTRADOR)
                    .tipoDia(tipoDia)
                    .fechaEvento(cmd.getFechaVisita())
                    .estado(estadoInicial)
                    .precioHistorico(precioBase)
                    .descuentoAplicado(descuentoPorNino)
                    .totalPagado(totalPorNino)
                    .nombreNino(nino.getNombreNino())
                    .edadNino(nino.getEdadNino())
                    .nombreAcompanante(cmd.getNombreAcompanante())
                    .dniAcompanante(cmd.getDniAcompanante())
                    .firmoConsentimiento(cmd.isActaFirmada())
                    .ingresado(ingresado)
                    .ingresoAt(ingresoAt)
                    .esReprogramacion(false)
                    .vecesReprogramada(0)
                    .createdBy(usuarioActual)
                    .build());
            reservas.add(reserva);
        }

        List<VentaPago> pagosGuardados = new ArrayList<>();
        for (PagoMostradorCommand pagoCmd : cmd.getPagos()) {
            VentaPago pago = ventaPagoRepository.save(VentaPago.builder()
                    .ventaId(ventaGuardada.getId())
                    .medioPagoCodigo(pagoCmd.getMedioPago())
                    .monto(pagoCmd.getMonto())
                    .referencia(pagoCmd.getReferencia())
                    .esValidado(true)
                    .validadoPor(usuarioActual)
                    .validadoAt(OffsetDateTime.now())
                    .build());
            pagosGuardados.add(pago);
        }

        for (VentaPago pago : pagosGuardados) {
            enrutadorCajaService.registrarIngresoEfectivo(
                    usuarioActual, pago.getMedioPagoCodigo(), pago.getMonto(),
                    "Venta mostrador #" + ventaGuardada.getId(), ventaGuardada.getId());
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                usuarioActual, AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_VENTAS,
                "Venta", ventaGuardada.getId(),
                null, "total=" + ventaGuardada.getTotal() + " | ninos=" + cmd.getNinos().size(),
                "Venta mostrador #" + ventaGuardada.getId() + " | " + cmd.getNinos().size() + " niño(s)",
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        List<VentaMostradorQuery.TicketMostradorQuery> tickets = reservas.stream()
                .map(r -> VentaMostradorQuery.TicketMostradorQuery.builder()
                        .reservaId(r.getId())
                        .numeroTicket(r.getNumeroTicket())
                        .codigoQr(r.getCodigoQr())
                        .nombreNino(r.getNombreNino())
                        .edadNino(r.getEdadNino())
                        .build())
                .toList();

        List<VentaMostradorQuery.PagoMostradorResultQuery> pagoResults = pagosGuardados.stream()
                .map(p -> VentaMostradorQuery.PagoMostradorResultQuery.builder()
                        .pagoId(p.getId())
                        .medioPago(p.getMedioPagoCodigo())
                        .monto(p.getMonto())
                        .build())
                .toList();

        return VentaMostradorQuery.builder()
                .ventaId(ventaGuardada.getId())
                .sedeId(ventaGuardada.getIdSede())
                .fechaVisita(ventaGuardada.getFechaVisita())
                .subtotal(ventaGuardada.getSubtotal())
                .descuento(ventaGuardada.getDescuento())
                .total(ventaGuardada.getTotal())
                .efectivoRecibido(ventaGuardada.getEfectivoRecibido())
                .vuelto(ventaGuardada.getVuelto())
                .createdAt(ventaGuardada.getCreatedAt())
                .tickets(tickets)
                .pagos(pagoResults)
                .build();
    }
}
