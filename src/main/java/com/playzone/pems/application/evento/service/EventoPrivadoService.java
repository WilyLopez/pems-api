package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarPagoCuotaCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarSaldoCommand;
import com.playzone.pems.application.evento.dto.command.SolicitarEventoPrivadoCommand;
import com.playzone.pems.application.evento.dto.command.VentaPagoItem;
import com.playzone.pems.application.evento.dto.query.EventoCuotaQuery;
import com.playzone.pems.application.evento.dto.query.EventoExtraQuery;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.dto.query.KpisEventosQuery;
import com.playzone.pems.application.evento.port.in.BuscarEventosAdminUseCase;
import com.playzone.pems.application.evento.port.in.CancelarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.in.CompletarEventoUseCase;
import com.playzone.pems.application.evento.port.in.ConfirmarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.in.ConsultarEventosPrivadosUseCase;
import com.playzone.pems.application.evento.port.in.RegistrarPagoCuotaUseCase;
import com.playzone.pems.application.evento.port.in.RegistrarSaldoUseCase;
import com.playzone.pems.application.evento.port.in.SolicitarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.out.EnviarNotificacionEventoPort;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.exception.FechaNoDisponibleException;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.EventoExtra;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.TipoEventoRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.model.VentaPago;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.FechaUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventoPrivadoService
        implements SolicitarEventoPrivadoUseCase,
        ConfirmarEventoPrivadoUseCase,
        CancelarEventoPrivadoUseCase,
        ConsultarEventosPrivadosUseCase,
        BuscarEventosAdminUseCase,
        CompletarEventoUseCase,
        RegistrarSaldoUseCase,
        RegistrarPagoCuotaUseCase {

    private final EventoPrivadoRepository         eventoRepository;
    private final ReservaPublicaRepository        reservaRepository;
    private final ClientePerfilRepository         clientePerfilRepository;
    private final BloqueCalendarioRepository      bloqueRepository;
    private final FeriadoRepository               feriadoRepository;
    private final TurnoRepository                 turnoRepository;
    private final ConfiguracionCalendarioRepository configRepository;
    private final EnviarNotificacionEventoPort    notificacionPort;
    private final EventoExtraRepository           eventoExtraRepository;
    private final VentaRepository                 ventaRepository;
    private final VentaPagoRepository             ventaPagoRepository;
    private final SupabaseAuthFacade              supabaseAuthFacade;
    private final ExtraPaqueteRepository          extraPaqueteRepository;
    private final ServicioCotizacionRepository    servicioCotizacionRepository;
    private final TipoEventoRepository            tipoEventoRepository;
    private final EventoCuotaRepository           cuotaRepository;
    private final ChecklistEventoRepository       checklistRepository;
    private final PerfilUsuarioRepository          perfilUsuarioRepository;
    private final EnrutadorCajaService             enrutadorCajaService;
    private final RegistrarLogUseCase              auditoria;
    private final CrearNotificacionPort            crearNotificacionPort;
    private final SedeScopeValidator               sedeScope;
    private final ObjectMapper                     objectMapper;

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> buscar(
            Long idSede, String estado,
            LocalDate fechaDesde, LocalDate fechaHasta,
            String tipoEvento, String modalidadPago,
            String search, Pageable pageable) {

        sedeScope.validarAccesoFiltro(idSede);

        EstadoEventoPrivado estadoEnum = null;
        if (estado != null && !estado.isBlank()) {
            try { estadoEnum = EstadoEventoPrivado.valueOf(estado); }
            catch (IllegalArgumentException ignored) {}
        }
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase() + "%" : null;
        String tipoFiltro = (tipoEvento != null && !tipoEvento.isBlank()) ? tipoEvento : null;
        String modalidadFiltro = (modalidadPago != null && !modalidadPago.isBlank()) ? modalidadPago : null;

        return eventoRepository.buscarAdmin(
                        idSede, estadoEnum, fechaDesde, fechaHasta,
                        tipoFiltro, modalidadFiltro, searchPattern, pageable)
                .map(e -> toQuery(e, obtenerCliente(e.getIdCliente()), obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public KpisEventosQuery kpis(Long idSede) {
        sedeScope.validarAccesoFiltro(idSede);
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
        return KpisEventosQuery.builder()
                .solicitadas(eventoRepository.countBySedeAndEstado(idSede, EstadoEventoPrivado.SOLICITADA))
                .confirmadas(eventoRepository.countBySedeAndEstado(idSede, EstadoEventoPrivado.CONFIRMADA))
                .completadasEsteMes(eventoRepository.countBySedeAndRangoAndEstado(
                        idSede, inicioMes, finMes, EstadoEventoPrivado.COMPLETADA))
                .conSaldoPendiente(eventoRepository.countConfirmadosConSaldo(idSede))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> consultarPorCliente(Long idCliente, Pageable pageable) {
        return eventoRepository.findByCliente(idCliente, pageable)
                .map(e -> toQuery(e, obtenerCliente(e.getIdCliente()), obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> consultarPorSedeYEstado(Long idSede, String estado, Pageable pageable) {
        sedeScope.validarAcceso(idSede);
        return eventoRepository.findBySedeAndEstado(idSede, EstadoEventoPrivado.valueOf(estado), pageable)
                .map(e -> toQuery(e, obtenerCliente(e.getIdCliente()), obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> consultarPorSedeYRangoFechas(Long idSede, LocalDate inicio, LocalDate fin, Pageable pageable) {
        sedeScope.validarAcceso(idSede);
        return eventoRepository.findBySedeAndFechasBetween(idSede, inicio, fin, pageable)
                .map(e -> toQuery(e, obtenerCliente(e.getIdCliente()), obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public EventoPrivadoQuery consultarPorId(Long idEvento) {
        EventoPrivado e = obtenerEvento(idEvento);
        if (!supabaseAuthFacade.tieneRol("CLIENTE")) {
            sedeScope.validarAcceso(e.getIdSede());
        }
        return toQuery(e, obtenerCliente(e.getIdCliente()), obtenerTurno(e.getIdTurno()), true);
    }

    // ─── Solicitar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(SolicitarEventoPrivadoCommand command) {
        if (!supabaseAuthFacade.tieneRol("CLIENTE")) {
            sedeScope.validarAcceso(command.getIdSede());
        }
        validarTipoEvento(command.getTipoEvento());
        validarFechaEvento(command.getIdSede(), command.getFechaEvento());
        validarTurnoEvento(command.getIdSede(), command.getFechaEvento(), command.getIdTurno());

        EventoPrivado evento = EventoPrivado.builder()
                .idCliente(command.getIdCliente())
                .idSede(command.getIdSede())
                .estado(EstadoEventoPrivado.SOLICITADA)
                .idTurno(command.getIdTurno())
                .fechaEvento(command.getFechaEvento())
                .tipoEvento(command.getTipoEvento())
                .contactoAdicional(command.getContactoAdicional())
                .origenContacto(command.getOrigenContacto())
                .aforoDeclarado(command.getAforoDeclarado())
                .montoAdelanto(BigDecimal.ZERO)
                .nombreNino(command.getNombreNino())
                .edadCumple(command.getEdadCumple())
                .notasInternas(command.getObservaciones())
                .paqueteId(command.getIdPaquete())
                .descripcionPersonalizada(command.getDescripcionPersonalizada())
                .presupuestoEstimado(command.getPresupuestoEstimado())
                .esCotizacionPersonalizada(command.isEsCotizacionPersonalizada())
                .modalidadPago("AL_CONTADO")
                .build();

        EventoPrivado guardado = eventoRepository.save(evento);
        persistirExtras(guardado.getId(), command.getIdsExtras(), command.getExtrasLibres());
        persistirServiciosCotizacion(guardado.getId(), command.getIdsServiciosCotizacion());

        ClientePerfil cliente = obtenerCliente(guardado.getIdCliente());
        Turno         turno   = obtenerTurno(guardado.getIdTurno());
        EventoPrivadoQuery query = toQuery(guardado, cliente, turno, true);

        notificacionPort.notificarAdminNuevaSolicitud(query);

        crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("EVENTO_PRESUPUESTO_ENVIADO")
                .destinatarioClienteId(guardado.getIdCliente())
                .entidadTipo("evento_privado")
                .entidadId(guardado.getId())
                .datosExtra(Map.of(
                        "fecha", guardado.getFechaEvento().toString(),
                        "tipo",  guardado.getTipoEvento()))
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                supabaseAuthFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_EVENTOS,
                "EventoPrivado", guardado.getId(),
                null, "cliente=" + guardado.getIdCliente() + " | fecha=" + guardado.getFechaEvento(),
                "Evento privado #" + guardado.getId() + " solicitado para " + guardado.getFechaEvento(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        return query;
    }

    // ─── Confirmar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(ConfirmarEventoCommand command) {
        EventoPrivado evento = obtenerEvento(command.getIdEvento());
        sedeScope.validarAcceso(evento.getIdSede());

        if (evento.getEstado() != EstadoEventoPrivado.SOLICITADA) {
            throw new ValidationException("Solo se pueden confirmar eventos en estado SOLICITADA.");
        }

        String modalidad = command.getModalidadPago() != null ? command.getModalidadPago() : "AL_CONTADO";

        BigDecimal adelanto = command.getMontoAdelanto() != null
                ? command.getMontoAdelanto() : BigDecimal.ZERO;

        if (adelanto.compareTo(command.getPrecioTotal()) > 0) {
            throw new ValidationException("montoAdelanto",
                    "El adelanto no puede superar el precio total del contrato.");
        }

        if ("CUOTAS".equals(modalidad)) {
            validarParametrosCuotas(command);
        }

        EventoPrivado confirmado = evento.toBuilder()
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .precioContrato(command.getPrecioTotal())
                .montoAdelanto(adelanto)
                .idUsuarioGestor(command.getIdUsuarioGestor())
                .modalidadPago(modalidad)
                .fechaLimitePago(command.getFechaLimitePago())
                .build();

        EventoPrivado guardado = eventoRepository.save(confirmado);

        checklistRepository.crearTareasBase(guardado.getId());

        // Registrar venta del adelanto si corresponde
        Venta ventaAdelanto = null;
        if (adelanto.compareTo(BigDecimal.ZERO) > 0 && !command.getPagosAdelanto().isEmpty()) {
            ventaAdelanto = crearVenta(guardado, "ADELANTO_EVENTO", adelanto, command.getIdUsuarioGestor());
            registrarPagos(ventaAdelanto.getId(), command.getPagosAdelanto(), command.getIdUsuarioGestor());
        }

        // Crear cronograma de cuotas
        if ("CUOTAS".equals(modalidad)) {
            crearCuotas(guardado, adelanto, command, ventaAdelanto);
        }

        ClientePerfil cliente = obtenerCliente(guardado.getIdCliente());
        Turno         turno   = obtenerTurno(guardado.getIdTurno());
        EventoPrivadoQuery query = toQuery(guardado, cliente, turno, true);

        crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("EVENTO_CONFIRMADO")
                .destinatarioClienteId(guardado.getIdCliente())
                .entidadTipo("evento_privado")
                .entidadId(guardado.getId())
                .build());

        if (adelanto.compareTo(BigDecimal.ZERO) > 0) {
            crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                    .tipoCodigo("PAGO_ADELANTO_CONFIRMADO")
                    .destinatarioClienteId(guardado.getIdCliente())
                    .entidadTipo("evento_privado")
                    .entidadId(guardado.getId())
                    .datosExtra(Map.of(
                            "monto", adelanto.toPlainString(),
                            "fecha", guardado.getFechaEvento().toString()))
                    .build());
            if (command.getIdUsuarioGestor() != null) {
                crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                        .tipoCodigo("EVENTO_ADELANTO_RECIBIDO")
                        .destinatarioUsuarioId(command.getIdUsuarioGestor())
                        .entidadTipo("evento_privado")
                        .entidadId(guardado.getId())
                        .datosExtra(Map.of(
                                "monto",   adelanto.toPlainString(),
                                "cliente", cliente.nombreCompleto(),
                                "evento",  query.getTipoEvento(),
                                "fecha",   guardado.getFechaEvento().toString()))
                        .build());
            }
        }

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                command.getIdUsuarioGestor(), AuditoriaConstants.ACCION_CONFIRMAR, AuditoriaConstants.MOD_EVENTOS,
                "EventoPrivado", guardado.getId(),
                EstadoEventoPrivado.SOLICITADA.getCodigo(), EstadoEventoPrivado.CONFIRMADA.getCodigo(),
                "Evento #" + guardado.getId() + " confirmado | precio=" + command.getPrecioTotal() + " | modalidad=" + modalidad,
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        return query;
    }

    // ─── Registrar pago de cuota ──────────────────────────────────────────────

    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(RegistrarPagoCuotaCommand command) {
        EventoCuota cuota = cuotaRepository.findById(command.getIdCuota())
                .orElseThrow(() -> new ResourceNotFoundException("EventoCuota", command.getIdCuota()));

        if (!cuota.esPendiente()) {
            throw new ValidationException("La cuota " + cuota.getNumeroCuota() + " ya está en estado " + cuota.getEstado() + ".");
        }

        BigDecimal totalPago = command.getPagos().stream()
                .map(VentaPagoItem::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPago.compareTo(cuota.getMonto()) != 0) {
            throw new ValidationException("El total de los pagos (" + totalPago +
                    ") no coincide con el monto de la cuota (" + cuota.getMonto() + ").");
        }

        EventoPrivado evento = eventoRepository.findByIdForUpdate(cuota.getEventoId())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", cuota.getEventoId()));
        sedeScope.validarAcceso(evento.getIdSede());

        Venta ventaSaldo = crearVenta(evento, "SALDO_EVENTO", totalPago, command.getIdUsuario());
        registrarPagos(ventaSaldo.getId(), command.getPagos(), command.getIdUsuario());

        // Marcar cuota como pagada
        cuotaRepository.save(cuota.toBuilder()
                .estado("PAGADO")
                .ventaId(ventaSaldo.getId())
                .build());

        // Acumular en montoAdelanto del evento
        BigDecimal nuevoAdelanto = evento.getMontoAdelanto().add(totalPago);
        EventoPrivado actualizado = evento.toBuilder().montoAdelanto(nuevoAdelanto).build();
        EventoPrivado guardado = eventoRepository.save(actualizado);

        ClientePerfil cliente = obtenerCliente(guardado.getIdCliente());
        enviarNotificacionAbono(guardado, cliente, totalPago);

        return toQuery(guardado, cliente, obtenerTurno(guardado.getIdTurno()), true);
    }

    // ─── Registrar saldo (pago libre, sin cuotas) ─────────────────────────────

    @Override
    @Transactional
    public EventoPrivadoQuery registrarSaldo(RegistrarSaldoCommand command) {
        EventoPrivado evento = eventoRepository.findByIdForUpdate(command.getIdEvento())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", command.getIdEvento()));
        sedeScope.validarAcceso(evento.getIdSede());

        if (command.getMonto() == null || command.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("monto", "El monto del saldo debe ser mayor a 0.");
        }
        if (evento.getPrecioContrato() == null) {
            throw new ValidationException("El evento aun no tiene un precio de contrato definido.");
        }
        BigDecimal saldoPendiente = evento.calcularMontoSaldo();
        if (command.getMonto().compareTo(saldoPendiente) > 0) {
            throw new ValidationException("monto",
                    "El monto (" + command.getMonto() + ") supera el saldo pendiente (" + saldoPendiente + ").");
        }

        Venta ventaSaldo = crearVenta(evento, "SALDO_EVENTO", command.getMonto(), command.getIdUsuario());
        ventaPagoRepository.save(VentaPago.builder()
                .ventaId(ventaSaldo.getId())
                .medioPagoCodigo(command.getMedioPago())
                .monto(command.getMonto())
                .esValidado(true)
                .validadoPor(command.getIdUsuario())
                .validadoAt(OffsetDateTime.now())
                .build());
        enrutadorCajaService.registrarIngresoEfectivoAdministrativo(
                command.getIdUsuario(), command.getMedioPago(), command.getMonto(),
                "Saldo evento #" + evento.getId(), ventaSaldo.getId());

        BigDecimal nuevoAdelanto = evento.getMontoAdelanto().add(command.getMonto());
        EventoPrivado guardado = eventoRepository.save(evento.toBuilder().montoAdelanto(nuevoAdelanto).build());

        ClientePerfil cliente = obtenerCliente(guardado.getIdCliente());
        enviarNotificacionAbono(guardado, cliente, command.getMonto());

        return toQuery(guardado, cliente, obtenerTurno(guardado.getIdTurno()), false);
    }

    private void enviarNotificacionAbono(EventoPrivado guardado, ClientePerfil cliente, BigDecimal monto) {
        BigDecimal saldo = guardado.calcularMontoSaldo() != null ? guardado.calcularMontoSaldo() : BigDecimal.ZERO;

        if (guardado.getIdUsuarioGestor() != null) {
            crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                    .tipoCodigo("EVENTO_SALDO_RECIBIDO")
                    .destinatarioUsuarioId(guardado.getIdUsuarioGestor())
                    .entidadTipo("evento_privado")
                    .entidadId(guardado.getId())
                    .datosExtra(Map.of(
                            "monto",   monto.toPlainString(),
                            "cliente", cliente.nombreCompleto(),
                            "evento",  guardado.getNombreTipoEvento() != null ? guardado.getNombreTipoEvento() : guardado.getTipoEvento(),
                            "saldo",   saldo.toPlainString()))
                    .build());
        }

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(Map.of("montoAbonado", monto.toPlainString()));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo serializar el monto del abono.", e);
        }

        crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("EVENTO_ABONO_RECIBIDO")
                .destinatarioClienteId(guardado.getIdCliente())
                .entidadTipo("evento_privado")
                .entidadId(guardado.getId())
                .datosExtra(Map.of(
                        "monto", monto.toPlainString(),
                        "fecha", guardado.getFechaEvento().toString(),
                        "saldo", saldo.toPlainString()))
                .metadata(metadataJson)
                .build());
    }

    // ─── Completar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EventoPrivadoQuery completar(Long idEvento, UUID idUsuarioGestor) {
        EventoPrivado evento = obtenerEvento(idEvento);
        sedeScope.validarAcceso(evento.getIdSede());
        if (evento.getEstado() != EstadoEventoPrivado.CONFIRMADA) {
            throw new ValidationException("Solo se pueden completar eventos en estado CONFIRMADA.");
        }
        if (LocalDate.now().isBefore(evento.getFechaEvento())) {
            throw new ValidationException("No se puede completar un evento que aun no ha ocurrido.");
        }
        EventoPrivado guardado = eventoRepository.save(evento.toBuilder()
                .estado(EstadoEventoPrivado.COMPLETADA)
                .idUsuarioGestor(idUsuarioGestor)
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                idUsuarioGestor, AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_EVENTOS,
                "EventoPrivado", idEvento,
                EstadoEventoPrivado.CONFIRMADA.getCodigo(), EstadoEventoPrivado.COMPLETADA.getCodigo(),
                "Evento #" + idEvento + " marcado como completado",
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        return toQuery(guardado, obtenerCliente(guardado.getIdCliente()), obtenerTurno(guardado.getIdTurno()), false);
    }

    // ─── Cancelar ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(Long idEvento, String motivoCancelacion) {
        EventoPrivado evento = obtenerEvento(idEvento);
        sedeScope.validarAcceso(evento.getIdSede());

        if (!evento.puedeCancelarse()) {
            throw new ValidationException("El evento no puede cancelarse en su estado actual.");
        }
        if (motivoCancelacion == null || motivoCancelacion.isBlank()) {
            throw new ValidationException("motivoCancelacion", "El motivo de cancelacion es obligatorio.");
        }

        EventoPrivado guardado = eventoRepository.save(evento.toBuilder()
                .estado(EstadoEventoPrivado.CANCELADA)
                .motivoCancelacion(motivoCancelacion)
                .build());

        ClientePerfil cliente = obtenerCliente(guardado.getIdCliente());
        Turno         turno   = obtenerTurno(guardado.getIdTurno());
        EventoPrivadoQuery query = toQuery(guardado, cliente, turno, false);

        crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("EVENTO_CANCELADO_ADMIN")
                .destinatarioClienteId(guardado.getIdCliente())
                .entidadTipo("evento_privado")
                .entidadId(idEvento)
                .datosExtra(Map.of(
                        "fecha",  guardado.getFechaEvento().toString(),
                        "motivo", motivoCancelacion))
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                supabaseAuthFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_CANCELAR, AuditoriaConstants.MOD_EVENTOS,
                "EventoPrivado", idEvento,
                evento.getEstado().getCodigo(), EstadoEventoPrivado.CANCELADA.getCodigo(),
                "Evento #" + idEvento + " cancelado: " + motivoCancelacion,
                null, null, AuditoriaConstants.NIVEL_CRITICAL, AuditoriaConstants.RESULTADO_EXITOSO));

        return query;
    }

    // ─── Helpers de negocio ───────────────────────────────────────────────────

    private void validarParametrosCuotas(ConfirmarEventoCommand command) {
        if (command.getNumeroCuotas() == null || command.getNumeroCuotas() < 2) {
            throw new ValidationException("numeroCuotas",
                    "Se requiere al menos 2 cuotas para la modalidad CUOTAS.");
        }
        if (command.getFechaLimitePago() == null) {
            throw new ValidationException("fechaLimitePago",
                    "Se requiere fecha límite de pago para la modalidad CUOTAS.");
        }
        if (!command.getFechaLimitePago().isAfter(LocalDate.now())) {
            throw new ValidationException("fechaLimitePago",
                    "La fecha límite de pago debe ser futura.");
        }
        BigDecimal adelanto = command.getMontoAdelanto() != null ? command.getMontoAdelanto() : BigDecimal.ZERO;
        if (adelanto.compareTo(command.getPrecioTotal()) >= 0) {
            throw new ValidationException("montoAdelanto",
                    "El adelanto no puede cubrir el 100% del precio en modalidad CUOTAS.");
        }
    }

    /**
     * Crea el cronograma de cuotas.
     * Cuota 1 = adelanto (PAGADO si ventaAdelanto != null, PENDIENTE si montoAdelanto = 0).
     * Cuotas 2..N = partes iguales del saldo restante, distribuidas hasta fechaLimitePago.
     */
    private void crearCuotas(EventoPrivado evento, BigDecimal adelanto,
                              ConfirmarEventoCommand command, Venta ventaAdelanto) {
        int n = command.getNumeroCuotas();
        LocalDate hoy = LocalDate.now();
        LocalDate limite = command.getFechaLimitePago();
        BigDecimal restante = command.getPrecioTotal().subtract(adelanto);

        List<EventoCuota> cuotas = new ArrayList<>();

        // Cuota 1: adelanto
        cuotas.add(EventoCuota.builder()
                .eventoId(evento.getId())
                .numeroCuota(1)
                .monto(adelanto.compareTo(BigDecimal.ZERO) > 0 ? adelanto : restante.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP))
                .fechaVencimiento(hoy)
                .estado(ventaAdelanto != null ? "PAGADO" : "PENDIENTE")
                .ventaId(ventaAdelanto != null ? ventaAdelanto.getId() : null)
                .build());

        // Cuotas 2..N: distribuir el saldo restante
        int cuotasRestantes = n - 1;
        BigDecimal montoPorCuota = restante.divide(BigDecimal.valueOf(cuotasRestantes), 2, RoundingMode.FLOOR);
        BigDecimal acumulado = montoPorCuota.multiply(BigDecimal.valueOf(cuotasRestantes - 1));
        BigDecimal montoUltima = restante.subtract(acumulado);

        long diasTotal = ChronoUnit.DAYS.between(hoy, limite);

        for (int i = 2; i <= n; i++) {
            long diasOffset = cuotasRestantes == 1 ? diasTotal
                    : diasTotal * (i - 1) / cuotasRestantes;
            BigDecimal monto = (i == n) ? montoUltima : montoPorCuota;

            cuotas.add(EventoCuota.builder()
                    .eventoId(evento.getId())
                    .numeroCuota(i)
                    .monto(monto)
                    .fechaVencimiento(hoy.plusDays(diasOffset))
                    .estado("PENDIENTE")
                    .build());
        }

        cuotaRepository.saveAll(cuotas);
    }

    private Venta crearVenta(EventoPrivado evento, String tipo, BigDecimal monto, UUID idUsuario) {
        return ventaRepository.save(Venta.builder()
                .idSede(evento.getIdSede())
                .clienteId(evento.getIdCliente())
                .eventoId(evento.getId())
                .tipo(tipo)
                .canalCodigo("MOSTRADOR")
                .subtotal(monto)
                .descuento(BigDecimal.ZERO)
                .total(monto)
                .efectivoRecibido(BigDecimal.ZERO)
                .vuelto(BigDecimal.ZERO)
                .actaFirmada(false)
                .esAnticipada(false)
                .createdBy(idUsuario)
                .build());
    }

    private void registrarPagos(Long ventaId, List<VentaPagoItem> pagos, UUID idUsuario) {
        pagos.forEach(p -> {
            ventaPagoRepository.save(VentaPago.builder()
                    .ventaId(ventaId)
                    .medioPagoCodigo(p.getMedioPagoCodigo())
                    .monto(p.getMonto())
                    .esValidado(true)
                    .validadoPor(idUsuario)
                    .validadoAt(OffsetDateTime.now())
                    .build());
            enrutadorCajaService.registrarIngresoEfectivoAdministrativo(
                    idUsuario, p.getMedioPagoCodigo(), p.getMonto(),
                    "Cobro evento venta #" + ventaId, ventaId);
        });
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private void validarTipoEvento(String tipoEventoCodigo) {
        if (tipoEventoCodigo == null || tipoEventoCodigo.isBlank()) {
            throw new ValidationException("tipoEvento", "El tipo de evento es obligatorio.");
        }
        var tipoEventoOpt = tipoEventoRepository.buscarPorCodigo(tipoEventoCodigo);
        if (tipoEventoOpt.isEmpty()) {
            throw new ValidationException("tipoEvento", "El tipo de evento especificado no existe.");
        }
        if (!tipoEventoOpt.get().isActivo()) {
            throw new ValidationException("tipoEvento", "El tipo de evento especificado no está activo.");
        }
    }

    private void validarFechaEvento(Long idSede, LocalDate fecha) {
        ConfiguracionCalendario cfg = configRepository.obtener(idSede);
        long dias = FechaUtil.diasEntre(FechaUtil.hoy(), fecha);

        boolean esAdmin = supabaseAuthFacade.tieneRol("ADMIN") || supabaseAuthFacade.tieneRol("SUPERADMIN");

        if (!esAdmin) {
            if (dias < cfg.getDiasMinEventoPrivado()) {
                throw new FechaNoDisponibleException(fecha,
                        "Los eventos privados deben reservarse con un minimo de "
                        + cfg.getDiasMinEventoPrivado()
                        + " dias de anticipacion. Por favor selecciona una fecha posterior.");
            }
            if (dias > cfg.getDiasMaxEventoPrivado()) {
                throw new ValidationException(
                        "Los eventos solo pueden agendarse hasta " + cfg.getDiasMaxEventoPrivado() + " dias adelante.");
            }
        }

        if (feriadoRepository.existsByFecha(fecha)) {
            throw new FechaNoDisponibleException(fecha, "Esta fecha es feriado.");
        }
        if (bloqueRepository.existsBloqueActivoEnFecha(idSede, fecha)) {
            throw new FechaNoDisponibleException(fecha, "La fecha esta bloqueada.");
        }
        if (reservaRepository.existsActivaBySedeAndFecha(idSede, fecha)) {
            throw new ValidationException(
                    "Esta fecha ya tiene reservas publicas y no admite eventos privados.");
        }
    }

    private void validarTurnoEvento(Long idSede, LocalDate fecha, Long idTurno) {
        Turno turno = turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ValidationException("Turno no encontrado."));
        if (eventoRepository.existsActivoBySedeAndFechaAndCodigoTurno(idSede, fecha, turno.getCodigo())) {
            throw new ValidationException(
                    "Este turno ya tiene un evento privado. Elige otro turno u otra fecha.");
        }
    }

    // ─── Persistencia auxiliar ────────────────────────────────────────────────

    private void persistirServiciosCotizacion(Long idEvento, List<Long> idsServicios) {
        if (idsServicios == null || idsServicios.isEmpty()) return;
        List<EventoExtra> extras = servicioCotizacionRepository.findAllActivos().stream()
                .filter(s -> idsServicios.contains(s.getId()))
                .map(s -> EventoExtra.builder()
                        .idEventoPrivado(idEvento)
                        .nombreLibre("Servicio: " + s.getNombre())
                        .build())
                .toList();
        if (!extras.isEmpty()) eventoExtraRepository.saveAll(extras);
    }

    private void persistirExtras(Long idEvento, List<Long> idsExtras, List<String> extrasLibres) {
        List<EventoExtra> extras = new ArrayList<>();
        if (idsExtras != null) {
            idsExtras.forEach(idExtra -> extras.add(
                    EventoExtra.builder().idEventoPrivado(idEvento).idExtra(idExtra).build()));
        }
        if (extrasLibres != null) {
            extrasLibres.stream().filter(t -> t != null && !t.isBlank()).forEach(texto -> extras.add(
                    EventoExtra.builder().idEventoPrivado(idEvento).nombreLibre(texto).build()));
        }
        if (!extras.isEmpty()) eventoExtraRepository.saveAll(extras);
    }

    // ─── Mapeo a Query ────────────────────────────────────────────────────────

    private EventoPrivado obtenerEvento(Long id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", id));
    }

    private ClientePerfil obtenerCliente(Long idCliente) {
        return clientePerfilRepository.buscarPorId(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", idCliente));
    }

    private Turno obtenerTurno(Long idTurno) {
        return turnoRepository.findById(idTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", idTurno));
    }

    private EventoPrivadoQuery toQuery(EventoPrivado e, ClientePerfil c, Turno t, boolean cargarDetalle) {
        List<EventoExtraQuery> extras = null;
        List<EventoCuotaQuery> cuotas = null;

        if (cargarDetalle) {
            extras = eventoExtraRepository.findByEvento(e.getId()).stream()
                    .map(ex -> {
                        String nombre = null;
                        if (ex.getIdExtra() != null) {
                            nombre = extraPaqueteRepository.findById(ex.getIdExtra())
                                    .map(ep -> ep.getNombre()).orElse(null);
                        }
                        return EventoExtraQuery.builder()
                                .id(ex.getId())
                                .idExtra(ex.getIdExtra())
                                .nombreExtra(nombre)
                                .nombreLibre(ex.getNombreLibre())
                                .build();
                    }).toList();

            if ("CUOTAS".equals(e.getModalidadPago())) {
                cuotas = cuotaRepository.findByEventoId(e.getId()).stream()
                        .map(c2 -> EventoCuotaQuery.builder()
                                .id(c2.getId())
                                .numeroCuota(c2.getNumeroCuota())
                                .monto(c2.getMonto())
                                .fechaVencimiento(c2.getFechaVencimiento())
                                .estado(c2.getEstado())
                                .ventaId(c2.getVentaId())
                                .createdAt(c2.getCreatedAt())
                                .build())
                        .toList();
            }
        }

        return EventoPrivadoQuery.builder()
                .id(e.getId())
                .idCliente(e.getIdCliente())
                .nombreCliente(c.nombreCompleto())
                .correoCliente(c.getCorreo())
                .telefonoCliente(c.getTelefono())
                .idSede(e.getIdSede())
                .estado(e.getEstado().getCodigo())
                .idTurno(e.getIdTurno())
                .turno(t.getDescripcion())
                .horaInicio(t.getHoraInicio().toString())
                .horaFin(t.getHoraFin().toString())
                .fechaEvento(e.getFechaEvento())
                .tipoEvento(e.getNombreTipoEvento() != null ? e.getNombreTipoEvento() : e.getTipoEvento())
                .contactoAdicional(e.getContactoAdicional())
                .origenContacto(e.getOrigenContacto())
                .motivoCancelacion(e.getMotivoCancelacion())
                .aforoDeclarado(e.getAforoDeclarado())
                .precioTotalContrato(e.getPrecioContrato())
                .montoAdelanto(e.getMontoAdelanto())
                .montoSaldo(e.calcularMontoSaldo())
                .observaciones(e.getNotasInternas())
                .nombreNino(e.getNombreNino())
                .edadCumple(e.getEdadCumple())
                .idPaquete(e.getPaqueteId())
                .descripcionPersonalizada(e.getDescripcionPersonalizada())
                .presupuestoEstimado(e.getPresupuestoEstimado())
                .esCotizacionPersonalizada(e.isEsCotizacionPersonalizada())
                .usuarioGestor(e.getIdUsuarioGestor() != null
                        ? perfilUsuarioRepository.buscarPorId(e.getIdUsuarioGestor())
                                .map(PerfilUsuario::getNombreCompleto)
                                .orElse(null)
                        : null)
                .estadoOperativo(e.getEstadoOperativo())
                .checklistCompleto(e.isChecklistCompleto())
                .horaInicioReal(e.getHoraInicioReal())
                .horaFinReal(e.getHoraFinReal())
                .extras(extras)
                .medioPago(fetchMedioPagoEvento(e.getId()))
                .fechaCreacion(e.getCreatedAt())
                .modalidadPago(e.getModalidadPago())
                .fechaLimitePago(e.getFechaLimitePago())
                .cuotas(cuotas)
                .build();
    }

    private String fetchMedioPagoEvento(Long idEvento) {
        List<Venta> ventas = ventaRepository.findByEventoId(idEvento);
        if (ventas.isEmpty()) return null;
        List<VentaPago> pagos = ventas.stream()
                .flatMap(v -> ventaPagoRepository.findByVentaId(v.getId()).stream())
                .toList();
        if (pagos.isEmpty()) return null;
        long distinct = pagos.stream().map(VentaPago::getMedioPagoCodigo).distinct().count();
        return distinct == 1 ? pagos.get(0).getMedioPagoCodigo() : "MULTIPLE";
    }
}
