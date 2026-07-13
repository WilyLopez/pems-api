package com.playzone.pems.interfaces.rest.evento.request;

import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.shared.validation.DniValidator;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CrearReservaRequest {

    @NotNull
    private CanalReserva canalReserva;

    @NotNull @FutureOrPresent
    private LocalDate fechaEvento;

    @NotBlank @Size(max = 120)
    private String nombreNino;

    @NotNull @Min(0) @Max(17)
    private Integer edadNino;

    @NotBlank @Size(max = 120)
    private String nombreAcompanante;

    @NotBlank @DniValidator
    private String dniAcompanante;

    @NotNull
    private Boolean firmoConsentimiento;

    private Long idPromocionManual;

    private String medioPago;
}