package com.playzone.pems.interfaces.rest.usuario.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompletarPerfilClienteRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombres;

    private String apellidoPaterno;
    private String apellidoMaterno;

    @NotBlank(message = "El tipo de documento es obligatorio")
    private String tipoDocumento;

    @Pattern(regexp = "^(\\d{8})?$", message = "El número de documento debe tener 8 dígitos")
    private String numeroDocumento;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(regexp = "^(9\\d{8})?$", message = "El teléfono debe comenzar con 9 y tener exactamente 9 dígitos")
    private String telefono;

    private boolean aceptaComunicaciones;
}
