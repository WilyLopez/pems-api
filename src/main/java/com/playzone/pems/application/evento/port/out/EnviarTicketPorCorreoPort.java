package com.playzone.pems.application.evento.port.out;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;

public interface EnviarTicketPorCorreoPort {

    void enviarTicket(String destinatario, String nombreCliente, ReservaPublicaQuery reserva);

    void enviarReservaPendiente(String destinatario, String nombreCliente, ReservaPublicaQuery reserva);

    void enviarReservaRechazada(String destinatario, String nombreCliente, ReservaPublicaQuery reserva, String motivo);

    void enviarReservaCancelada(String destinatario, String nombreCliente, ReservaPublicaQuery reserva, String motivo);
}