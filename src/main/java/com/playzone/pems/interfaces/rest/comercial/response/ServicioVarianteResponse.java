package com.playzone.pems.interfaces.rest.comercial.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ServicioVarianteResponse {
    private final Long       id;
    private final Long       idServicio;
    private final String     nombre;
    private final String     descripcion;
    private final BigDecimal precio;
    private final boolean    activo;
    private final int        orden;
}
