package com.playzone.pems.interfaces.rest.evento.mapper;

import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.interfaces.rest.evento.response.EventoPrivadoResponse;
import org.springframework.stereotype.Component;

@Component
public class EventoPrivadoResponseMapper {

    public EventoPrivadoResponse toResponse(EventoPrivadoQuery q) {
        return EventoPrivadoResponse.builder()
                .id(q.getId())
                .idCliente(q.getIdCliente())
                .nombreCliente(q.getNombreCliente())
                .correoCliente(q.getCorreoCliente())
                .telefonoCliente(q.getTelefonoCliente())
                .idSede(q.getIdSede())
                .estado(q.getEstado())
                .idTurno(q.getIdTurno())
                .turno(q.getTurno())
                .horaInicio(q.getHoraInicio())
                .horaFin(q.getHoraFin())
                .fechaEvento(q.getFechaEvento())
                .tipoEvento(q.getTipoEvento())
                .contactoAdicional(q.getContactoAdicional())
                .origenContacto(q.getOrigenContacto())
                .motivoCancelacion(q.getMotivoCancelacion())
                .aforoDeclarado(q.getAforoDeclarado())
                .precioTotalContrato(q.getPrecioTotalContrato())
                .montoAdelanto(q.getMontoAdelanto())
                .montoSaldo(q.getMontoSaldo())
                .observaciones(q.getObservaciones())
                .nombreNino(q.getNombreNino())
                .edadCumple(q.getEdadCumple())
                .idPaquete(q.getIdPaquete())
                .descripcionPersonalizada(q.getDescripcionPersonalizada())
                .presupuestoEstimado(q.getPresupuestoEstimado())
                .esCotizacionPersonalizada(q.isEsCotizacionPersonalizada())
                .usuarioGestor(q.getUsuarioGestor())
                .estadoOperativo(q.getEstadoOperativo())
                .checklistCompleto(q.isChecklistCompleto())
                .horaInicioReal(q.getHoraInicioReal())
                .horaFinReal(q.getHoraFinReal())
                .extras(q.getExtras())
                .servicios(q.getServicios())
                .medioPago(q.getMedioPago())
                .fechaCreacion(q.getFechaCreacion())
                .modalidadPago(q.getModalidadPago())
                .fechaLimitePago(q.getFechaLimitePago())
                .cuotas(q.getCuotas())
                .build();
    }
}
