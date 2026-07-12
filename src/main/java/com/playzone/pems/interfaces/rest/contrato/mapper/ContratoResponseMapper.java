package com.playzone.pems.interfaces.rest.contrato.mapper;

import com.playzone.pems.application.contrato.dto.query.ContratoQuery;
import com.playzone.pems.interfaces.rest.contrato.response.ContratoResponse;
import org.springframework.stereotype.Component;

@Component
public class ContratoResponseMapper {

    public ContratoResponse toResponse(ContratoQuery q) {
        return ContratoResponse.builder()
                .id(q.getId())
                .idEventoPrivado(q.getIdEventoPrivado())
                .usuarioCarga(q.getUsuarioCarga())
                .fechaCarga(q.getFechaCarga())
                .nombreCliente(q.getNombreCliente())
                .correoCliente(q.getCorreoCliente())
                .tipoEvento(q.getTipoEvento())
                .fechaEvento(q.getFechaEvento())
                .turno(q.getTurno())
                .aforoDeclarado(q.getAforoDeclarado())
                .precioTotalContrato(q.getPrecioTotalContrato())
                .montoAdelanto(q.getMontoAdelanto())
                .saldoPendiente(q.getSaldoPendiente())
                .actividades(q.getActividades() == null ? null : q.getActividades().stream()
                        .map(a -> ContratoResponse.ActividadContratoResponse.builder()
                                .id(a.getId())
                                .accion(a.getAccion())
                                .descripcion(a.getDescripcion())
                                .usuario(a.getUsuario())
                                .fechaAccion(a.getFechaAccion())
                                .build()).toList())
                .build();
    }
}
