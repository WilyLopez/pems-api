package com.playzone.pems.application.notificacion.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoEntregaQuery {
    private String entidadTipo;
    private Long entidadId;
    private String estado;
    private OffsetDateTime fechaEnvio;
    private String mensajeError;
}
