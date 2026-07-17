package com.playzone.pems.interfaces.rest.comercial.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class CategoriaServicioResponse {
    private final Long    id;
    private final String  nombre;
    private final int     orden;
    private final boolean activo;
    private final OffsetDateTime fechaCreacion;
    private final OffsetDateTime fechaActualizacion;
}
