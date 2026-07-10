package com.playzone.pems.interfaces.rest.finanzas.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AnularRegistroRequest {

    @NotBlank(message = "El motivo de anulacion es obligatorio.")
    @Size(max = 300)
    private String motivo;
}
