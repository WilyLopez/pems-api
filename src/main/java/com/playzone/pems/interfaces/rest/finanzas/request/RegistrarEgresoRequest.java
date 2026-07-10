package com.playzone.pems.interfaces.rest.finanzas.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class RegistrarEgresoRequest {

    @NotBlank
    private String tipoEgresoCodigo;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 10, fraction = 2)
    private BigDecimal monto;

    @NotNull
    private LocalDate fecha;

    @NotBlank(message = "El medio de pago es obligatorio.")
    @Pattern(regexp = "EFECTIVO|YAPE|TARJETA|PLIN|TRANSFERENCIA",
            message = "Medio de pago no valido.")
    private String medioPago;

    private Integer periodoAnio;
    private Integer periodoMes;
    private String  descripcion;
    private String  comprobanteUrl;
    private boolean esRecurrente = false;
}
