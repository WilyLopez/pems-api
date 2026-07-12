package com.playzone.pems.interfaces.rest.evento.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoEntregaCorreoResponse {
    private String estado;
    private OffsetDateTime fechaEnvio;
    private String mensajeError;
}
