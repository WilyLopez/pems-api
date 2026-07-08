package com.playzone.pems.application.evento.port.out;

import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;

public interface EnviarNotificacionEventoPort {

    void notificarSolicitudRecibida(String destinatario, EventoPrivadoQuery evento);

    void notificarEventoConfirmado(String destinatario, EventoPrivadoQuery evento);

    void notificarEventoCancelado(String destinatario, EventoPrivadoQuery evento, String motivo);

    void notificarAdminNuevaSolicitud(EventoPrivadoQuery evento);

    void notificarAbonoRecibido(String destinatario, EventoPrivadoQuery evento, java.math.BigDecimal montoAbonado, java.math.BigDecimal saldoRestante);
}
