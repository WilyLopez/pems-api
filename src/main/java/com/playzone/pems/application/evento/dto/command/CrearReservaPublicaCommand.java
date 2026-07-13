package com.playzone.pems.application.evento.dto.command;

import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.shared.validation.DniValidator;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CrearReservaPublicaCommand {

    @NotNull
    private final Long idCliente;

    @NotNull
    private final Long idSede;

    @NotNull
    private final CanalReserva canalReserva;

    @NotNull
    @Future
    private final LocalDate fechaEvento;

    @NotBlank
    @Size(max = 120)
    private final String nombreNino;

    @NotNull
    @Min(0) @Max(17)
    private final Integer edadNino;

    @NotBlank
    @Size(max = 120)
    private final String nombreAcompanante;

    @NotBlank
    @DniValidator
    private final String dniAcompanante;

    @NotNull
    private final Boolean firmoConsentimiento;

    private final Long idPromocionManual;

    private final String medioPago;
}