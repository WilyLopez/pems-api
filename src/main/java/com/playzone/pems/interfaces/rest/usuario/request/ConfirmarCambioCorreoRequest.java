package com.playzone.pems.interfaces.rest.usuario.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ConfirmarCambioCorreoRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;
}
