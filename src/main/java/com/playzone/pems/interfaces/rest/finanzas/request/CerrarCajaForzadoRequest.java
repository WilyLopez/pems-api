package com.playzone.pems.interfaces.rest.finanzas.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CerrarCajaForzadoRequest {

    @PositiveOrZero
    private BigDecimal saldoFinal;

    @NotBlank
    private String motivo;

    private String observaciones;
}
