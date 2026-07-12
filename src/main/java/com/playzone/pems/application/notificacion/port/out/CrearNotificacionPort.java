package com.playzone.pems.application.notificacion.port.out;

import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;

public interface CrearNotificacionPort {

    void notificar(CrearNotificacionCommand cmd);

    void notificarTransaccional(CrearNotificacionCommand cmd);
}
