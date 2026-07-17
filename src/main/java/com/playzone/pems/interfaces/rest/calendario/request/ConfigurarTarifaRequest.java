package com.playzone.pems.interfaces.rest.calendario.request;

import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class ConfigurarTarifaRequest {

    @NotNull
    private TipoDia tipoDia;

    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2)
    private BigDecimal precio;

    @Min(1)
    private Integer duracionMinutos;

    @NotNull @FutureOrPresent
    private LocalDate vigenciaDesde;

    private LocalDate vigenciaHasta;
}