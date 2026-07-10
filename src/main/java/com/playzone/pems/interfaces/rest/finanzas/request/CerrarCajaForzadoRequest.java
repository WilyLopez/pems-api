package com.playzone.pems.interfaces.rest.finanzas.request;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CerrarCajaForzadoRequest {

    @NotNull(message = "El conteo fisico del efectivo es obligatorio.")
    @PositiveOrZero
    @Digits(integer = 10, fraction = 2)
    private BigDecimal saldoFinal;

    @NotBlank
    private String motivo;

    private String observaciones;
}
