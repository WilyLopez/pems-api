package com.playzone.pems.interfaces.rest.finanzas.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArqueoCajaResponse {
    private Long           id;
    private Long           idSesionCaja;
    private BigDecimal     saldoEsperado;
    private BigDecimal     saldoContado;
    private BigDecimal     diferencia;
    private String         observaciones;
    private UUID           realizadoPor;
    private OffsetDateTime fechaCreacion;
}
