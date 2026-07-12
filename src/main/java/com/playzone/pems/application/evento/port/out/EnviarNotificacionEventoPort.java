package com.playzone.pems.application.evento.port.out;

import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;

public interface EnviarNotificacionEventoPort {

    void notificarAdminNuevaSolicitud(EventoPrivadoQuery evento);
}
