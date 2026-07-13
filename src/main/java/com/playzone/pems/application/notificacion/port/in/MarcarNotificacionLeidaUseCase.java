package com.playzone.pems.application.notificacion.port.in;

import java.util.UUID;

public interface MarcarNotificacionLeidaUseCase {

    void marcarLeidaUsuario(Long id, UUID usuarioId);

    void marcarLeidaCliente(Long id, Long clienteId);

    void marcarTodasLeidasUsuario(UUID usuarioId);

    void marcarTodasLeidasCliente(Long clienteId);

    void eliminarCliente(Long id, Long clienteId);
}
