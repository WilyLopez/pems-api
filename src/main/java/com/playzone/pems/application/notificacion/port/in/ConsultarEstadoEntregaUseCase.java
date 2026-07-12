package com.playzone.pems.application.notificacion.port.in;

import com.playzone.pems.application.notificacion.dto.query.EstadoEntregaQuery;

public interface ConsultarEstadoEntregaUseCase {

    EstadoEntregaQuery consultarPorEntidad(String entidadTipo, Long entidadId);
}
