package com.playzone.pems.interfaces.rest.usuario.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ActualizarClientePerfilRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "El apellido paterno no puede exceder 100 caracteres")
    private String apellidoPaterno;

    @Size(max = 100, message = "El apellido materno no puede exceder 100 caracteres")
    private String apellidoMaterno;

    @Size(max = 20, message = "El teléfono no puede exceder 20 caracteres")
    @Pattern(
        regexp = "^(9\\d{8})?$",
        message = "El teléfono debe comenzar con 9 y tener exactamente 9 dígitos"
    )
    private String telefono;

    @Pattern(regexp = "^(\\d{8})?$", message = "El número de documento debe tener 8 dígitos")
    private String numeroDocumento;

    @Email(message = "El correo debe ser válido")
    @Size(max = 254, message = "El correo no puede exceder 254 caracteres")
    private String correo;

    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    private Boolean aceptaComunicaciones;
}
