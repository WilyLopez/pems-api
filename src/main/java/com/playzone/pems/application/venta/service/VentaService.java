package com.playzone.pems.application.venta.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;
import com.playzone.pems.application.venta.dto.command.CobrarReservaCommand;
import com.playzone.pems.application.venta.dto.command.PagoMostradorCommand;
import com.playzone.pems.application.venta.dto.query.VentaQuery;
import com.playzone.pems.application.venta.port.in.ConsultarVentasUseCase;
import com.playzone.pems.application.venta.port.in.ProcesarVentaUseCase;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.application.venta.port.out.EnviarDocumentosVentaPort;
import com.playzone.pems.domain.venta.exception.VentaNotFoundException;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class VentaService implements ProcesarVentaUseCase, ConsultarVentasUseCase {

    private final VentaRepository          ventaRepository;
    private final VentaPagoRepository      ventaPagoRepository;
    private final ClientePerfilRepository  clientePerfilRepository;
    private final ReservaPublicaRepository reservaPublicaRepository;
    private final EnviarDocumentosVentaPort enviarDocumentosVentaPort;
    private final ConfiguracionCalendarioRepository configRepository;
    private final EnrutadorCajaService     enrutadorCajaService;
    private final RegistrarLogUseCase      auditoria;

    @Override
    @Transactional
    public VentaQuery cobrarReserva(CobrarReservaCommand command) {
        ReservaPublica reserva = reservaPublicaRepository.findById(command.getReservaId())
                .orElseThrow(() -> new ValidationException("Reserva no encontrada."));

        if (reserva.getEstado() != EstadoReservaPublica.PENDIENTE) {
            throw new ValidationException("Solo se pueden cobrar reservas en estado PENDIENTE.");
        }

        java.time.ZoneId zoneId = java.time.ZoneId.of("America/Lima");
        java.time.LocalTime horaActual = java.time.LocalTime.now(zoneId);
        java.time.LocalTime apertura = java.time.LocalTime.of(10, 0);
        java.time.LocalTime cierre = java.time.LocalTime.of(20, 0);

        try {
            var config = configRepository.obtener(reserva.getIdSede());
            if (config != null) {
                if (config.getHoraApertura() != null) apertura = config.getHoraApertura();
                if (config.getHoraCierre() != null) cierre = config.getHoraCierre();
            }
        } catch (Exception ignored) {}

        if (horaActual.isBefore(apertura) || horaActual.isAfter(cierre)) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.ENGLISH);
            throw new ValidationException(String.format("El local está cerrado. Solo se permiten registrar ventas dentro del horario de atención: %s - %s.",
                    apertura.format(formatter), cierre.format(formatter)));
        }

        boolean esHoy = reserva.getFechaEvento().equals(LocalDate.now(zoneId));

        BigDecimal efectivoRecibido = command.getEfectivoRecibido() != null ? command.getEfectivoRecibido() : BigDecimal.ZERO;
        BigDecimal vuelto = VentaPagoValidator.validarYCalcularVuelto(command.getPagos(), reserva.getTotalPagado(), efectivoRecibido);

        Venta venta;
        if (reserva.getVentaId() != null) {
            Venta existente = ventaRepository.findById(reserva.getVentaId())
                    .orElseThrow(() -> new ValidationException("Venta asociada no encontrada."));
            venta = existente.toBuilder()
                    .canalCodigo("MOSTRADOR")
                    .efectivoRecibido(efectivoRecibido)
                    .vuelto(vuelto)
                    .actaFirmada(command.isActaFirmada())
                    .esAnticipada(reserva.getFechaEvento().isAfter(LocalDate.now(zoneId)))
                    .notas(command.getNotas())
                    .build();
        } else {
            venta = Venta.builder()
                    .idSede(reserva.getIdSede())
                    .clienteId(reserva.getIdCliente())
                    .tipo("RESERVA")
                    .canalCodigo("MOSTRADOR")
                    .fechaVisita(reserva.getFechaEvento())
                    .nombreAcompanante(reserva.getNombreAcompanante())
                    .dniAcompanante(reserva.getDniAcompanante())
                    .subtotal(reserva.getPrecioHistorico())
                    .descuento(reserva.getDescuentoAplicado())
                    .total(reserva.getTotalPagado())
                    .efectivoRecibido(efectivoRecibido)
                    .vuelto(vuelto)
                    .actaFirmada(command.isActaFirmada())
                    .esAnticipada(reserva.getFechaEvento().isAfter(LocalDate.now(zoneId)))
                    .notas(command.getNotas())
                    .createdBy(command.getCreatedBy())
                    .build();
        }

        Venta ventaGuardada = ventaRepository.save(venta);
        ventaPagoRepository.deleteByVentaId(ventaGuardada.getId());

        for (PagoMostradorCommand pagoCmd : command.getPagos()) {
            ventaPagoRepository.save(VentaPago.builder()
                    .ventaId(ventaGuardada.getId())
                    .medioPagoCodigo(pagoCmd.getMedioPago())
                    .monto(pagoCmd.getMonto())
                    .referencia(pagoCmd.getReferencia())
                    .esValidado(true)
                    .validadoPor(command.getCreatedBy())
                    .validadoAt(OffsetDateTime.now())
                    .build());
            enrutadorCajaService.registrarIngresoEfectivo(
                    command.getCreatedBy(), pagoCmd.getMedioPago(), pagoCmd.getMonto(),
                    "Cobro reserva venta #" + ventaGuardada.getId(), ventaGuardada.getId());
        }

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

        ReservaPublica reservaActualizada = reserva.toBuilder()
            .ventaId(ventaGuardada.getId())
            .firmoConsentimiento(command.isActaFirmada())
            .estado(estadoInicial)
            .ingresado(ingresado)
            .ingresoAt(ingresoAt)
            .build();
        
        reservaPublicaRepository.save(reservaActualizada);

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getCreatedBy(), AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_VENTAS,
                "Venta", ventaGuardada.getId(),
                null, "total=" + ventaGuardada.getTotal(),
                "Cobro de reserva #" + command.getReservaId() + " | venta #" + ventaGuardada.getId(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        return toQuery(ventaGuardada);
    }

    @Override
    @Transactional
    public void marcarImpreso(Long idVenta) {
        Venta v = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new VentaNotFoundException(idVenta));
        ventaRepository.save(v.toBuilder().impreso(true).build());
    }

    @Override
    @Transactional
    public void marcarDescargado(Long idVenta) {
        Venta v = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new VentaNotFoundException(idVenta));
        ventaRepository.save(v.toBuilder().descargado(true).build());
    }

    @Override
    @Transactional
    public void enviarCorreoVenta(Long idVenta, String correo) {
        VentaDetalleQuery detalle = consultarDetallePorId(idVenta);
        String destinatario = correo;
        if (destinatario == null || destinatario.trim().isEmpty()) {
            if (detalle.getClienteId() != null) {
                destinatario = clientePerfilRepository.buscarPorId(detalle.getClienteId())
                        .map(c -> c.getCorreo())
                        .orElse(null);
            }
        }
        if (destinatario == null || destinatario.trim().isEmpty()) {
            throw new ValidationException("No se encontro un correo destinatario para enviar los documentos.");
        }
        enviarDocumentosVentaPort.enviarDocumentos(destinatario.trim(), detalle);

        Venta v = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new VentaNotFoundException(idVenta));
        ventaRepository.save(v.toBuilder().enviadoCorreo(true).build());
    }

    @Override
    @Transactional(readOnly = true)
    public VentaQuery consultarPorId(Long idVenta) {
        return toQuery(ventaRepository.findById(idVenta)
                .orElseThrow(() -> new VentaNotFoundException(idVenta)));
    }

    @Override
    @Transactional(readOnly = true)
    public VentaDetalleQuery consultarDetallePorId(Long idVenta) {
        Venta v = ventaRepository.findById(idVenta)
                .orElseThrow(() -> new VentaNotFoundException(idVenta));

        String nombreCliente = null;
        if (v.getClienteId() != null) {
            nombreCliente = clientePerfilRepository.buscarPorId(v.getClienteId())
                    .map(c -> c.nombreCompleto())
                    .orElse(null);
        }

        var tickets = reservaPublicaRepository.findByVentaId(idVenta).stream()
                .map(this::mapReservaToQuery)
                .toList();

        var pagos = ventaPagoRepository.findByVentaId(idVenta).stream()
                .map(p -> VentaDetalleQuery.PagoDetalleQuery.builder()
                        .id(p.getId())
                        .medioPago(p.getMedioPagoCodigo())
                        .monto(p.getMonto())
                        .referencia(p.getReferencia())
                        .esValidado(p.isEsValidado())
                        .build())
                .toList();

        BigDecimal totalPagado = pagos.stream()
                .map(VentaDetalleQuery.PagoDetalleQuery::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return VentaDetalleQuery.builder()
                .id(v.getId())
                .idSede(v.getIdSede())
                .clienteId(v.getClienteId())
                .eventoId(v.getEventoId())
                .tipo(v.getTipo())
                .canalCodigo(v.getCanalCodigo())
                .fechaVisita(v.getFechaVisita())
                .subtotal(v.getSubtotal())
                .descuento(v.getDescuento())
                .total(v.getTotal())
                .nombreAcompanante(v.getNombreAcompanante())
                .dniAcompanante(v.getDniAcompanante())
                .telefonoAcompanante(v.getTelefonoAcompanante())
                .nombreCliente(nombreCliente)
                .notas(v.getNotas())
                .impreso(v.isImpreso())
                .enviadoCorreo(v.isEnviadoCorreo())
                .descargado(v.isDescargado())
                .efectivoRecibido(v.getEfectivoRecibido())
                .vuelto(v.getVuelto())
                .createdAt(v.getCreatedAt())
                .tickets(tickets)
                .pagos(pagos)
                .totalPagado(totalPagado)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VentaQuery> consultarPorSedeYFechas(
            Long idSede, LocalDate desde, LocalDate hasta, String search, Pageable pageable) {
        OffsetDateTime inicio = desde.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime fin    = hasta.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC);
        if (search != null && !search.trim().isEmpty()) {
            return ventaRepository.findBySedeAndFechasBetweenAndSearch(idSede, inicio, fin, search.trim(), pageable)
                    .map(this::toQuery);
        }
        return ventaRepository.findBySedeAndFechasBetween(idSede, inicio, fin, pageable)
                .map(this::toQuery);
    }

    private VentaQuery toQuery(Venta v) {
        String nombreCliente = null;
        if (v.getClienteId() != null) {
            nombreCliente = clientePerfilRepository.buscarPorId(v.getClienteId())
                    .map(c -> c.nombreCompleto())
                    .orElse(null);
        }

        return VentaQuery.builder()
                .id(v.getId())
                .idSede(v.getIdSede())
                .clienteId(v.getClienteId())
                .eventoId(v.getEventoId())
                .tipo(v.getTipo())
                .canalCodigo(v.getCanalCodigo())
                .fechaVisita(v.getFechaVisita())
                .subtotal(v.getSubtotal())
                .descuento(v.getDescuento())
                .total(v.getTotal())
                .nombreAcompanante(v.getNombreAcompanante())
                .dniAcompanante(v.getDniAcompanante())
                .nombreCliente(nombreCliente)
                .notas(v.getNotas())
                .impreso(v.isImpreso())
                .enviadoCorreo(v.isEnviadoCorreo())
                .descargado(v.isDescargado())
                .efectivoRecibido(v.getEfectivoRecibido())
                .vuelto(v.getVuelto())
                .createdAt(v.getCreatedAt())
                .build();
    }

    private ReservaPublicaQuery mapReservaToQuery(ReservaPublica r) {
        String nombreCliente = null;
        String correoCliente = null;
        if (r.getIdCliente() != null) {
            var cp = clientePerfilRepository.buscarPorId(r.getIdCliente()).orElse(null);
            if (cp != null) {
                nombreCliente = cp.nombreCompleto();
                correoCliente = cp.getCorreo();
            }
        }
        return ReservaPublicaQuery.builder()
                .id(r.getId())
                .idCliente(r.getIdCliente())
                .nombreCliente(nombreCliente)
                .correoCliente(correoCliente)
                .idSede(r.getIdSede())
                .nombreSede(null)
                .estado(r.getEstado() != null ? r.getEstado().getCodigo() : null)
                .canalReserva(r.getCanalReserva() != null ? r.getCanalReserva().getCodigo() : null)
                .tipoDia(r.getTipoDia() != null ? r.getTipoDia().getCodigo() : null)
                .fechaEvento(r.getFechaEvento())
                .numeroTicket(r.getNumeroTicket())
                .precioHistorico(r.getPrecioHistorico())
                .descuentoAplicado(r.getDescuentoAplicado())
                .totalPagado(r.getTotalPagado())
                .nombreNino(r.getNombreNino())
                .edadNino(r.getEdadNino())
                .nombreAcompanante(r.getNombreAcompanante())
                .dniAcompanante(r.getDniAcompanante())
                .firmoConsentimiento(r.isFirmoConsentimiento())
                .esReprogramacion(r.isEsReprogramacion())
                .vecesReprogramada(r.getVecesReprogramada())
                .ingresado(r.isIngresado())
                .fechaIngreso(r.getIngresoAt())
                .codigoQr(r.getCodigoQr())
                .fechaCreacion(r.getCreatedAt())
                .build();
    }
}
