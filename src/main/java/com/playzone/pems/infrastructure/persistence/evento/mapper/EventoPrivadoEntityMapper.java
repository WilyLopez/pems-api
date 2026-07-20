package com.playzone.pems.infrastructure.persistence.evento.mapper;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.ModalidadPago;
import com.playzone.pems.infrastructure.persistence.calendario.entity.TurnoEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class EventoPrivadoEntityMapper {

    public EventoPrivado toDomain(EventoPrivadoEntity e) {
        if (e == null) return null;
        return EventoPrivado.builder()
                .id(e.getId())
                .idCliente(e.getClienteId())
                .idSede(e.getSede().getId())
                .estado(e.getEstado())
                .idTurno(e.getTurno().getId())
                .codigoTurno(e.getTurno().getCodigo())
                .fechaEvento(e.getFechaEvento())
                .tipoEvento(e.getTipoEvento())
                .nombreTipoEvento(e.getTipoEventoRef() != null ? e.getTipoEventoRef().getNombre() : e.getTipoEvento())
                .contactoAdicional(e.getContactoAdicional())
                .origenContacto(e.getOrigenContacto())
                .aforoDeclarado(e.getAforoDeclarado())
                .precioContrato(e.getPrecioContrato())
                .montoAdelanto(e.getMontoAdelanto())
                .motivoCancelacion(e.getMotivoCancelacion())
                .notasInternas(e.getNotasInternas())
                .nombreNino(e.getNombreNino())
                .edadCumple(e.getEdadCumple())
                .paqueteId(e.getPaqueteId())
                .descripcionPersonalizada(e.getDescripcionPersonalizada())
                .presupuestoEstimado(e.getPresupuestoEstimado())
                .esCotizacionPersonalizada(e.isEsCotizacionPersonalizada())
                .idUsuarioGestor(e.getUsuarioGestorId())
                .estadoOperativo(e.getEstadoOperativo())
                .checklistCompleto(e.isChecklistCompleto())
                .horaInicioReal(e.getHoraInicioReal())
                .horaFinReal(e.getHoraFinReal())
                .modalidadPago(e.getModalidadPago())
                .fechaLimitePago(e.getFechaLimitePago())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }

    public EventoPrivadoEntity toEntity(EventoPrivado d,
                                         SedeEntity sede,
                                         TurnoEntity turno) {
        if (d == null) return null;
        return EventoPrivadoEntity.builder()
                .id(d.getId())
                .clienteId(d.getIdCliente())
                .sede(sede)
                .estado(d.getEstado())
                .turno(turno)
                .fechaEvento(d.getFechaEvento())
                .tipoEvento(d.getTipoEvento())
                .contactoAdicional(d.getContactoAdicional())
                .origenContacto(d.getOrigenContacto())
                .aforoDeclarado(d.getAforoDeclarado())
                .precioContrato(d.getPrecioContrato())
                .montoAdelanto(d.getMontoAdelanto())
                .motivoCancelacion(d.getMotivoCancelacion())
                .notasInternas(d.getNotasInternas())
                .nombreNino(d.getNombreNino())
                .edadCumple(d.getEdadCumple())
                .paqueteId(d.getPaqueteId())
                .descripcionPersonalizada(d.getDescripcionPersonalizada())
                .presupuestoEstimado(d.getPresupuestoEstimado())
                .esCotizacionPersonalizada(d.isEsCotizacionPersonalizada())
                .usuarioGestorId(d.getIdUsuarioGestor())
                .estadoOperativo(d.getEstadoOperativo())
                .checklistCompleto(d.isChecklistCompleto())
                .horaInicioReal(d.getHoraInicioReal())
                .horaFinReal(d.getHoraFinReal())
                .modalidadPago(d.getModalidadPago() != null ? d.getModalidadPago() : ModalidadPago.AL_CONTADO)
                .fechaLimitePago(d.getFechaLimitePago())
                .createdBy(d.getCreatedBy())
                .updatedBy(d.getUpdatedBy())
                .build();
    }
}
