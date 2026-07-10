package com.playzone.pems.domain.finanzas.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArqueoCaja {
    private Long           id;
    private Long           idSesionCaja;
    private BigDecimal     saldoEsperado;
    private BigDecimal     saldoContado;
    private BigDecimal     diferencia;
    private String         observaciones;
    private UUID           realizadoPor;
    private OffsetDateTime fechaCreacion;
}
