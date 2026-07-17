package com.playzone.pems.domain.comercial.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServicioVariante {
    private Long       id;
    private Long       idServicio;
    private String     nombre;
    private String     descripcion;
    private BigDecimal precio;
    private boolean    activo;
    private int        orden;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
