package com.playzone.pems.interfaces.rest.usuario.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActivarCuentaStaffRequest {

    @NotBlank(message = "El token es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String nuevaContrasena;

    @AssertTrue(message = "La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial (!@#$%&*?)")
    public boolean isPasswordValida() {
        if (nuevaContrasena == null || nuevaContrasena.length() < 8) return false;
        boolean tieneUpper    = nuevaContrasena.chars().anyMatch(Character::isUpperCase);
        boolean tieneLower    = nuevaContrasena.chars().anyMatch(Character::isLowerCase);
        boolean tieneDigit    = nuevaContrasena.chars().anyMatch(Character::isDigit);
        boolean tieneEspecial = nuevaContrasena.chars().anyMatch(c -> "!@#$%&*?".indexOf(c) >= 0);
        return tieneUpper && tieneLower && tieneDigit && tieneEspecial;
    }
}
