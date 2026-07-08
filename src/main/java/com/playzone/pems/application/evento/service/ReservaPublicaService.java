package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.command.CrearReservaPublicaCommand;
import com.playzone.pems.application.evento.dto.command.ReprogramarReservaCommand;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.CancelarReservaUseCase;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.application.evento.port.in.CrearReservaPublicaUseCase;
import com.playzone.pems.application.evento.port.in.ReprogramarReservaUseCase;
import com.playzone.pems.application.evento.port.out.EnviarTicketPorCorreoPort;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.exception.AforoExcedidoException;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.domain.calendario.exception.FechaNoDisponibleException;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.domain.evento.exception.ReservaNotFoundException;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.FechaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaPublicaService
        implements CrearReservaPublicaUseCase,
        ReprogramarReservaUseCase,
        CancelarReservaUseCase,
        ConsultarReservasUseCase {

    private final ReservaPublicaRepository          reservaRepository;
    private final EventoPrivadoRepository           eventoRepository;
    private final ClientePerfilRepository           clientePerfilRepository;
    private final SedeRepository                    sedeRepository;
    private final TarifaRepository                  tarifaRepository;
    private final FeriadoRepository                 feriadoRepository;
    private final BloqueCalendarioRepository        bloqueRepository;
    private final ConfiguracionCalendarioRepository configRepository;
    private final EnviarTicketPorCorreoPort         correoPort;
    private final StoragePort                       storagePort;
    private final VentaRepository                   ventaRepository;
    private final VentaPagoRepository               ventaPagoRepository;
    private final SupabaseAuthFacade                supabaseAuthFacade;
    private final ConfiguracionGlobalRepository     configuracionGlobalRepository;
    private final RegistrarLogUseCase               auditoria;
    private final CrearNotificacionPort             crearNotificacionPort;

    @org.springframework.beans.factory.annotation.Value("${supabase.storage.bucket-comprobantes:comprobantes}")
    private String bucketComprobantes;

    @org.springframework.beans.factory.annotation.Value("${supabase.storage.bucket-publico:kiki-publico}")
    private String bucketPublico;

    // ── Consultas ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaPublicaQuery> consultarPorCliente(Long idCliente, Pageable pageable) {
        ClientePerfil cliente = clientePerfilRepository.buscarPorId(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", idCliente));
        return reservaRepository.findByCliente(idCliente, pageable)
                .map(r -> toQuery(r, cliente.nombreCompleto(), cliente.getCorreo(), fetchNombreSede(r.getIdSede()), null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaPublicaQuery> consultarPorSedeYFecha(Long idSede, LocalDate fecha, Pageable pageable) {
        String nombreSede = fetchNombreSede(idSede);
        return reservaRepository.findBySedeAndFecha(idSede, fecha, pageable)
                .map(r -> toQuery(r, fetchNombreCliente(r.getIdCliente()), null, nombreSede, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservaPublicaQuery> consultarPorSedeYEstado(Long idSede, String estado, Pageable pageable) {
        String nombreSede = fetchNombreSede(idSede);
        return reservaRepository.findBySedeAndEstado(idSede, EstadoReservaPublica.valueOf(estado), pageable)
                .map(r -> toQuery(r, fetchNombreCliente(r.getIdCliente()), null, nombreSede, null, null));
    }

    @Transactional(readOnly = true)
    public ReservaPublicaQuery consultarPorId(Long idReserva) {
        ReservaPublica r = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaNotFoundException(idReserva));
        return enriquecerQuery(r);
    }

    @Transactional(readOnly = true)
    public ReservaPublicaQuery consultarPorNumeroTicket(String numeroTicket) {
        ReservaPublica r = reservaRepository.findByNumeroTicket(numeroTicket)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "numeroTicket", numeroTicket));
        return enriquecerQuery(r);
    }

    // ── Comandos ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReservaPublicaQuery ejecutar(CrearReservaPublicaCommand command) {
        validarFechaDisponible(command.getIdSede(), command.getFechaEvento());

        TipoDia tipoDia = resolverTipoDia(command.getFechaEvento());

        Tarifa tarifa = tarifaRepository
                .findVigenteBySedeAndTipoDiaAndFecha(command.getIdSede(), tipoDia, command.getFechaEvento())
                .orElseThrow(() -> new ValidationException("No existe tarifa vigente para esa fecha."));

        ClientePerfil cliente = clientePerfilRepository.buscarPorId(command.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", command.getIdCliente()));

        BigDecimal precio    = tarifa.getPrecio();
        BigDecimal descuento = BigDecimal.ZERO;
        BigDecimal total     = precio.subtract(descuento);

        Venta venta = ventaRepository.save(Venta.builder()
                .idSede(command.getIdSede())
                .clienteId(command.getIdCliente())
                .tipo("RESERVA")
                .canalCodigo("WEB")
                .fechaVisita(command.getFechaEvento())
                .subtotal(precio)
                .descuento(descuento)
                .total(total)
                .efectivoRecibido(BigDecimal.ZERO)
                .vuelto(BigDecimal.ZERO)
                .actaFirmada(false)
                .esAnticipada(true)
                .createdBy(cliente.getUsuarioId())
                .build());

        ReservaPublica reserva = ReservaPublica.builder()
                .ventaId(venta.getId())
                .idCliente(command.getIdCliente())
                .idSede(command.getIdSede())
                .estado(EstadoReservaPublica.PENDIENTE)
                .canalReserva(command.getCanalReserva())
                .tipoDia(tipoDia)
                .fechaEvento(command.getFechaEvento())
                .precioHistorico(precio)
                .descuentoAplicado(descuento)
                .totalPagado(total)
                .nombreNino(command.getNombreNino())
                .edadNino(command.getEdadNino())
                .nombreAcompanante(command.getNombreAcompanante())
                .dniAcompanante(command.getDniAcompanante())
                .firmoConsentimiento(command.getFirmoConsentimiento())
                .esReprogramacion(false)
                .vecesReprogramada(0)
                .build();

        ReservaPublica guardada = reservaRepository.save(reserva);

        ReservaPublicaQuery query = toQuery(guardada, cliente.nombreCompleto(), cliente.getCorreo(),
                fetchNombreSede(command.getIdSede()), null, null);
        if (cliente.getCorreo() != null) {
            correoPort.enviarReservaPendiente(cliente.getCorreo(), cliente.nombreCompleto(), query);
        } else {
            log.warn("Reserva {} sin correo de cliente {}, no se envia aviso de pendiente", guardada.getId(), command.getIdCliente());
        }

        crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                .tipoCodigo("TICKET_DISPONIBLE")
                .destinatarioClienteId(guardada.getIdCliente())
                .entidadTipo("reserva_publica")
                .entidadId(guardada.getId())
                .datosExtra(Map.of(
                        "fecha",  guardada.getFechaEvento().toString(),
                        "ticket", guardada.getNumeroTicket() != null ? guardada.getNumeroTicket() : ""))
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                cliente.getUsuarioId(), AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_RESERVAS,
                "ReservaPublica", guardada.getId(),
                null, "ticket=" + guardada.getNumeroTicket() + " | fecha=" + guardada.getFechaEvento(),
                "Reserva #" + guardada.getId() + " creada | ticket=" + guardada.getNumeroTicket(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        return query;
    }

    @Override
    @Transactional
    public ReservaPublicaQuery ejecutar(ReprogramarReservaCommand command) {
        ReservaPublica original = reservaRepository.findById(command.getIdReservaOriginal())
                .orElseThrow(() -> new ReservaNotFoundException(command.getIdReservaOriginal()));

        int maxReprogramaciones = configuracionGlobalRepository.findByClave("MAX_REPROGRAMACIONES")
                .map(c -> { try { return Integer.parseInt(c.getValor()); } catch (NumberFormatException e) { return 1; } })
                .orElse(1);

        if (!original.puedeReprogramarse(maxReprogramaciones)) {
            throw new ValidationException("La reserva no puede reprogramarse en su estado actual o supero el limite.");
        }

        validarFechaDisponible(original.getIdSede(), command.getNuevaFechaEvento());

        TipoDia tipoDia = resolverTipoDia(command.getNuevaFechaEvento());

        Tarifa tarifa = tarifaRepository
                .findVigenteBySedeAndTipoDiaAndFecha(
                        original.getIdSede(), tipoDia, command.getNuevaFechaEvento())
                .orElseThrow(() -> new ValidationException("No existe tarifa vigente para la nueva fecha."));

        ReservaPublica originalActualizada = original.toBuilder()
                .estado(EstadoReservaPublica.REPROGRAMADA)
                .build();
        reservaRepository.save(originalActualizada);

        BigDecimal precio = tarifa.getPrecio();

        ReservaPublica nueva = ReservaPublica.builder()
                .idCliente(original.getIdCliente())
                .idSede(original.getIdSede())
                .estado(EstadoReservaPublica.PENDIENTE)
                .canalReserva(original.getCanalReserva())
                .tipoDia(tipoDia)
                .idReservaOriginal(original.getId())
                .esReprogramacion(true)
                .vecesReprogramada(original.getVecesReprogramada() + 1)
                .fechaEvento(command.getNuevaFechaEvento())
                .precioHistorico(precio)
                .descuentoAplicado(BigDecimal.ZERO)
                .totalPagado(precio)
                .nombreNino(original.getNombreNino())
                .edadNino(original.getEdadNino())
                .nombreAcompanante(original.getNombreAcompanante())
                .dniAcompanante(original.getDniAcompanante())
                .firmoConsentimiento(original.isFirmoConsentimiento())
                .build();

        ReservaPublica guardada = reservaRepository.save(nueva);

        ClientePerfil cliente = clientePerfilRepository.buscarPorId(guardada.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", guardada.getIdCliente()));

        ReservaPublicaQuery query = toQuery(guardada, cliente.nombreCompleto(), cliente.getCorreo(),
                fetchNombreSede(guardada.getIdSede()), null, null);
        if (cliente.getCorreo() != null) {
            correoPort.enviarTicket(cliente.getCorreo(), cliente.nombreCompleto(), query);
        } else {
            log.warn("Reprogramacion {} sin correo de cliente {}, no se envia ticket", guardada.getId(), guardada.getIdCliente());
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                supabaseAuthFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_REPROGRAMAR, AuditoriaConstants.MOD_RESERVAS,
                "ReservaPublica", guardada.getId(),
                "reservaOriginal=" + command.getIdReservaOriginal(), "nuevaFecha=" + command.getNuevaFechaEvento(),
                "Reserva #" + command.getIdReservaOriginal() + " reprogramada → nueva reserva #" + guardada.getId(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        return query;
    }

    @Override
    @Transactional
    public ReservaPublicaQuery ejecutar(Long idReserva, String motivo) {
        ReservaPublica reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaNotFoundException(idReserva));

        if (!reserva.puedeCancelarse()) {
            throw new ValidationException("La reserva no puede cancelarse en su estado actual.");
        }

        ReservaPublica cancelada = reserva.toBuilder()
                .estado(EstadoReservaPublica.CANCELADA)
                .motivoCancelacion(motivo)
                .build();

        ReservaPublica guardada = reservaRepository.save(cancelada);

        ClientePerfil cliente = clientePerfilRepository.buscarPorId(guardada.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", guardada.getIdCliente()));

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                supabaseAuthFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_CANCELAR, AuditoriaConstants.MOD_RESERVAS,
                "ReservaPublica", idReserva,
                reserva.getEstado().getCodigo(), "CANCELADA",
                "Reserva #" + idReserva + " cancelada: " + motivo,
                null, null, AuditoriaConstants.NIVEL_CRITICAL, AuditoriaConstants.RESULTADO_EXITOSO));

        ReservaPublicaQuery query = toQuery(guardada, cliente.nombreCompleto(), cliente.getCorreo(),
                fetchNombreSede(guardada.getIdSede()), null, null);
        if (cliente.getCorreo() != null) {
            correoPort.enviarReservaCancelada(cliente.getCorreo(), cliente.nombreCompleto(), query, motivo);
        }
        return query;
    }

    @Transactional
    public ReservaPublicaQuery confirmarPago(Long idReserva, String medioPago) {
        ReservaPublica reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaNotFoundException(idReserva));
        if (reserva.getEstado() != EstadoReservaPublica.PENDIENTE) {
            throw new ValidationException("Solo se pueden confirmar reservas en estado PENDIENTE.");
        }
        ReservaPublica guardada = reservaRepository.save(
                reserva.toBuilder().estado(EstadoReservaPublica.CONFIRMADA).build());
        
        ventaPagoRepository.deleteByVentaId(guardada.getVentaId());

        ventaPagoRepository.save(VentaPago.builder()
                .ventaId(guardada.getVentaId())
                .medioPagoCodigo(medioPago)
                .monto(guardada.getTotalPagado())
                .esValidado(true)
                .validadoPor(supabaseAuthFacade.usuarioActualId().orElse(null))
                .validadoAt(java.time.OffsetDateTime.now())
                .build());

        crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                .tipoCodigo("PAGO_CONFIRMADO")
                .destinatarioClienteId(guardada.getIdCliente())
                .entidadTipo("reserva_publica")
                .entidadId(guardada.getId())
                .datosExtra(Map.of(
                        "monto", guardada.getTotalPagado().toPlainString(),
                        "fecha", guardada.getFechaEvento().toString()))
                .build());

        ClientePerfil cliente = clientePerfilRepository.buscarPorId(guardada.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", guardada.getIdCliente()));
        if (cliente.getCorreo() != null) {
            correoPort.enviarTicket(cliente.getCorreo(), cliente.nombreCompleto(), enriquecerQuery(guardada));
        }
        return enriquecerQuery(guardada);
    }

    @Transactional
    public ReservaPublicaQuery rechazarPago(Long idReserva, String motivo) {
        ReservaPublica reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaNotFoundException(idReserva));
        if (reserva.getEstado() != EstadoReservaPublica.PENDIENTE) {
            throw new ValidationException("Solo se pueden rechazar pagos de reservas en estado PENDIENTE.");
        }
        
        var pagosExistentes = ventaPagoRepository.findByVentaId(reserva.getVentaId());
        for (var pago : pagosExistentes) {
            if (!pago.isEsValidado()) {
                ventaPagoRepository.save(pago.toBuilder()
                        .referencia(null)
                        .build());
            }
        }

        crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                .tipoCodigo("PAGO_RECHAZADO")
                .destinatarioClienteId(reserva.getIdCliente())
                .entidadTipo("reserva_publica")
                .entidadId(reserva.getId())
                .datosExtra(Map.of(
                        "motivo", motivo != null ? motivo : "Comprobante invalido",
                        "fecha", reserva.getFechaEvento().toString()))
                .build());

        ClientePerfil cliente = clientePerfilRepository.buscarPorId(reserva.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", reserva.getIdCliente()));
        if (cliente.getCorreo() != null) {
            correoPort.enviarReservaRechazada(cliente.getCorreo(), cliente.nombreCompleto(), enriquecerQuery(reserva), motivo);
        }
        return enriquecerQuery(reserva);
    }

    @Transactional
    public ReservaPublicaQuery actualizarReferenciaPago(Long idReserva, MultipartFile archivo) {
        String url;
        try {
            byte[] bytes = archivo.getBytes();
            String key = "comprobantes/" + UUID.randomUUID() + "_" + archivo.getOriginalFilename();
            String mime = archivo.getContentType() != null ? archivo.getContentType() : "application/octet-stream";
            url = storagePort.upload(bucketComprobantes, key, bytes, mime);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el comprobante", e);
        }
        ReservaPublica reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new ReservaNotFoundException(idReserva));

        var pagosExistentes = ventaPagoRepository.findByVentaId(reserva.getVentaId());
        boolean actualizado = false;
        for (var pago : pagosExistentes) {
            if (!pago.isEsValidado()) {
                ventaPagoRepository.save(pago.toBuilder()
                        .referencia(url)
                        .medioPagoCodigo("YAPE")
                        .build());
                actualizado = true;
                break;
            }
        }
        if (!actualizado) {
            ventaPagoRepository.save(VentaPago.builder()
                    .ventaId(reserva.getVentaId())
                    .medioPagoCodigo("YAPE")
                    .monto(reserva.getTotalPagado())
                    .referencia(url)
                    .esValidado(false)
                    .build());
        }

        return enriquecerQuery(reserva);
    }

    // ── Validaciones internas ────────────────────────────────────────────────────

    private void validarFechaDisponible(Long idSede, LocalDate fecha) {
        ConfiguracionCalendario cfg = configRepository.obtener(idSede);
        LocalDate hoy   = FechaUtil.hoy();
        LocalDate max   = hoy.plusDays(cfg.getDiasMaxReservaPublica());

        if (fecha.isBefore(hoy)) {
            throw new FechaNoDisponibleException(fecha, "No se puede reservar en fechas pasadas.");
        }
        if (fecha.isAfter(max)) {
            throw new ValidationException(
                    "Las reservas solo se permiten hasta " + cfg.getDiasMaxReservaPublica()
                    + " dias de anticipacion.");
        }
        if (fecha.isEqual(hoy) && FechaUtil.ahora().toLocalTime().isAfter(cfg.getHoraCierre())) {
            throw new FechaNoDisponibleException(fecha, "El local ya cerro por hoy. Elige otra fecha.");
        }
        if (bloqueRepository.existsBloqueActivoEnFecha(idSede, fecha)) {
            throw new FechaNoDisponibleException(fecha, "La fecha esta bloqueada.");
        }
        if (eventoRepository.existsActivoBySedeAndFecha(idSede, fecha)) {
            throw new ValidationException(
                    "Esta fecha esta reservada para un evento privado. Elige otra fecha.");
        }
        int activas = reservaRepository.countActivasBySedeAndFecha(idSede, fecha);
        if (activas >= cfg.getAforoMaximo()) {
            throw new AforoExcedidoException(fecha, cfg.getAforoMaximo());
        }
    }

    private TipoDia resolverTipoDia(LocalDate fecha) {
        boolean esFeriado = feriadoRepository.findByFecha(fecha).isPresent();
        return (FechaUtil.esFindeSemana(fecha) || esFeriado)
                ? TipoDia.FIN_SEMANA_FERIADO
                : TipoDia.SEMANA;
    }

    private String fetchNombreCliente(Long idCliente) {
        return clientePerfilRepository.buscarPorId(idCliente)
                .map(ClientePerfil::nombreCompleto)
                .orElse("Cliente Desconocido");
    }

    private String fetchNombreSede(Long idSede) {
        return sedeRepository.findById(idSede)
                .map(s -> s.getNombre())
                .orElse("Sede Principal");
    }

    // ── Mapeo ────────────────────────────────────────────────────────────────────

    private ReservaPublicaQuery enriquecerQuery(ReservaPublica r) {
        var cliente = clientePerfilRepository.buscarPorId(r.getIdCliente()).orElse(null);
        return toQuery(r,
                cliente != null ? cliente.nombreCompleto() : null,
                cliente != null ? cliente.getCorreo() : null,
                fetchNombreSede(r.getIdSede()),
                fetchMedioPago(r.getVentaId()),
                fetchReferenciaPago(r.getVentaId()));
    }

    private ReservaPublicaQuery toQuery(ReservaPublica r, String nombreCliente,
                                        String correoCliente, String nombreSede,
                                        String medioPago, String referenciaPago) {
        return ReservaPublicaQuery.builder()
                .id(r.getId())
                .idCliente(r.getIdCliente())
                .nombreCliente(nombreCliente)
                .correoCliente(correoCliente)
                .idSede(r.getIdSede())
                .nombreSede(nombreSede)
                .estado(r.getEstado().getCodigo())
                .canalReserva(r.getCanalReserva().getCodigo())
                .tipoDia(r.getTipoDia().getCodigo())
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
                .medioPago(medioPago)
                .referenciaPago(referenciaPago)
                .motivoCancelacion(r.getMotivoCancelacion())
                .fechaCreacion(r.getCreatedAt())
                .build();
    }

    private String fetchMedioPago(Long idVenta) {
        if (idVenta == null) return null;
        var pagos = ventaPagoRepository.findByVentaId(idVenta);
        if (pagos.isEmpty()) return null;
        if (pagos.size() == 1) return pagos.get(0).getMedioPagoCodigo();
        return "MULTIPLE";
    }

    private String fetchReferenciaPago(Long idVenta) {
        if (idVenta == null) return null;
        var pagos = ventaPagoRepository.findByVentaId(idVenta);
        if (pagos.isEmpty()) return null;
        if (pagos.size() == 1) return pagos.get(0).getReferencia();
        return null;
    }
}
