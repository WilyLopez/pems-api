package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.application.evento.dto.command.SolicitarEventoPrivadoCommand;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.port.in.CancelarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.in.CompletarEventoUseCase;
import com.playzone.pems.application.evento.port.in.ConfirmarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.in.SolicitarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.out.EnviarNotificacionEventoPort;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.model.enums.ModalidadPago;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventoPrivadoCicloVidaService
        implements SolicitarEventoPrivadoUseCase,
        ConfirmarEventoPrivadoUseCase,
        CancelarEventoPrivadoUseCase,
        CompletarEventoUseCase {

    private static final List<String> TAREAS_CHECKLIST_BASE = List.of(
            "Decoracion lista",
            "Sonido instalado",
            "Animador confirmado",
            "Catering preparado",
            "Personal asignado",
            "Area limpia y habilitada"
    );

    private final EventoPrivadoRepository      eventoRepository;
    private final EnviarNotificacionEventoPort notificacionPort;
    private final ChecklistEventoRepository    checklistRepository;
    private final SupabaseAuthFacade           supabaseAuthFacade;
    private final RegistrarLogUseCase          auditoria;
    private final CrearNotificacionPort        crearNotificacionPort;
    private final SedeScopeValidator           sedeScope;
    private final EventoAccesoValidator        accesoValidator;
    private final EventoPrivadoQueryMapper     mapper;
    private final VentaEventoWriter            ventaWriter;
    private final SolicitudEventoPrivadoValidator solicitudValidator;
    private final EventoServicioExtraWriter    extraWriter;
    private final EventoCuotaGenerator         cuotaGenerator;

    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(SolicitarEventoPrivadoCommand command) {
        accesoValidator.validarAccesoAlEvento(command.getIdCliente(), command.getIdSede());
        solicitudValidator.validarTipoEvento(command.getTipoEvento());
        solicitudValidator.validarFechaEvento(command.getIdSede(), command.getFechaEvento());
        solicitudValidator.validarTurnoEvento(command.getIdSede(), command.getFechaEvento(), command.getIdTurno());
        solicitudValidator.validarAforoYEdad(command.getIdSede(), command.getAforoDeclarado(), command.getEdadCumple());
        solicitudValidator.validarNombreYEdad(command.getNombreNino(), command.getEdadCumple());
        solicitudValidator.validarDescripcionPersonalizada(command.isEsCotizacionPersonalizada(), command.getDescripcionPersonalizada());

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
                .modalidadPago(ModalidadPago.AL_CONTADO)
                .build();

        EventoPrivado guardado = eventoRepository.save(evento);
        extraWriter.persistirExtras(guardado.getId(), command.getIdsExtras(), command.getExtrasLibres());
        extraWriter.persistirServiciosCotizacion(guardado.getId(), command.getIdsServiciosCotizacion(), command.getVariantesSeleccionadas());

        ClientePerfil cliente = mapper.obtenerCliente(guardado.getIdCliente());
        Turno         turno   = mapper.obtenerTurno(guardado.getIdTurno());
        EventoPrivadoQuery query = mapper.toQuery(guardado, cliente, turno, true);

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


    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(ConfirmarEventoCommand command) {
        EventoPrivado evento = mapper.obtenerEvento(command.getIdEvento());
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
            cuotaGenerator.validarParametrosCuotas(command);
        }

        EventoPrivado confirmado = evento.toBuilder()
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .precioContrato(command.getPrecioTotal())
                .montoAdelanto(adelanto)
                .idUsuarioGestor(command.getIdUsuarioGestor())
                .modalidadPago(ModalidadPago.desdeCodigo(modalidad))
                .fechaLimitePago(command.getFechaLimitePago())
                .build();

        EventoPrivado guardado = eventoRepository.save(confirmado);

        checklistRepository.crearTareasBase(guardado.getId(), TAREAS_CHECKLIST_BASE);

        Venta ventaAdelanto = null;
        if (adelanto.compareTo(BigDecimal.ZERO) > 0 && !command.getPagosAdelanto().isEmpty()) {
            ventaAdelanto = ventaWriter.crearVenta(guardado, "ADELANTO_EVENTO", adelanto, command.getIdUsuarioGestor());
            ventaWriter.registrarPagos(ventaAdelanto.getId(), command.getPagosAdelanto(), command.getIdUsuarioGestor());
        }

        if ("CUOTAS".equals(modalidad)) {
            cuotaGenerator.crearCuotas(guardado, adelanto, command, ventaAdelanto);
        }

        ClientePerfil cliente = mapper.obtenerCliente(guardado.getIdCliente());
        Turno         turno   = mapper.obtenerTurno(guardado.getIdTurno());
        EventoPrivadoQuery query = mapper.toQuery(guardado, cliente, turno, true);

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


    @Override
    @Transactional
    public EventoPrivadoQuery completar(Long idEvento, UUID idUsuarioGestor) {
        EventoPrivado evento = mapper.obtenerEvento(idEvento);
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

        return mapper.toQuery(guardado, mapper.obtenerCliente(guardado.getIdCliente()), mapper.obtenerTurno(guardado.getIdTurno()), false);
    }


    @Override
    @Transactional
    public EventoPrivadoQuery ejecutar(Long idEvento, String motivoCancelacion) {
        EventoPrivado evento = mapper.obtenerEvento(idEvento);
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

        ClientePerfil cliente = mapper.obtenerCliente(guardado.getIdCliente());
        Turno         turno   = mapper.obtenerTurno(guardado.getIdTurno());
        EventoPrivadoQuery query = mapper.toQuery(guardado, cliente, turno, false);

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
}
