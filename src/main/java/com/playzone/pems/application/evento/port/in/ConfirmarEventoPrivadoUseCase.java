package com.playzone.pems.application.evento.port.in;

import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;

public interface ConfirmarEventoPrivadoUseCase {

    EventoPrivadoQuery ejecutar(ConfirmarEventoCommand command);
}
