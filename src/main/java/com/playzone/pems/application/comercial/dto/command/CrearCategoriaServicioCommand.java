package com.playzone.pems.application.comercial.dto.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearCategoriaServicioCommand {

    @NotBlank
    @Size(max = 80)
    private String nombre;

    private boolean activo;

    @Min(0)
    private int orden;
}
