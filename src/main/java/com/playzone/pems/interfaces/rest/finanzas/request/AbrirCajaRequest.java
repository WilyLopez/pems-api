package com.playzone.pems.interfaces.rest.finanzas.request;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AbrirCajaRequest {

    @PositiveOrZero
    private BigDecimal saldoInicial;
    private String     observaciones;
}
