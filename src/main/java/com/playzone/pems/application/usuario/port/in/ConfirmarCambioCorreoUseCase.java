package com.playzone.pems.application.usuario.port.in;

public interface ConfirmarCambioCorreoUseCase {
    void confirmar(Long idCliente, String token);
}
