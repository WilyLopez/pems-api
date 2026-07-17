package com.playzone.pems.application.calendario.dto.command;

import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class ConfigurarTarifaCommand {

    @NotNull
    private final Long idSede;

    @NotNull
    private final TipoDia tipoDia;

    @NotNull
    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private final BigDecimal precio;

    @Min(1)
    private final Integer duracionMinutos;

    @NotNull
    @FutureOrPresent
    private final LocalDate vigenciaDesde;

    private final LocalDate vigenciaHasta;
}