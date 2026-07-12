package com.playzone.pems.application.usuario.port.in;

public interface ActivarCuentaStaffUseCase {
    void activarCuenta(String token, String nuevaContrasena);
}
