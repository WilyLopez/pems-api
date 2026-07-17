package com.playzone.pems.application.comercial.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioVarianteQuery {
    private Long       id;
    private Long       idServicio;
    private String     nombre;
    private String     descripcion;
    private BigDecimal precio;
    private boolean    activo;
    private int        orden;
}
