package com.playzone.pems.application.finanzas.dto.query;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class ArqueoCajaQuery {
    private Long           id;
    private Long           idSesionCaja;
    private BigDecimal     saldoEsperado;
    private BigDecimal     saldoContado;
    private BigDecimal     diferencia;
    private String         observaciones;
    private UUID           realizadoPor;
    private OffsetDateTime fechaCreacion;
}
