package com.playzone.pems.application.venta.port.in;

import com.playzone.pems.application.venta.dto.command.CobrarReservaCommand;
import com.playzone.pems.application.venta.dto.query.VentaQuery;

public interface ProcesarVentaUseCase {

    VentaQuery cobrarReserva(CobrarReservaCommand command);

    void marcarImpreso(Long idVenta);

    void marcarDescargado(Long idVenta);

    void enviarCorreoVenta(Long idVenta, String correo);
}