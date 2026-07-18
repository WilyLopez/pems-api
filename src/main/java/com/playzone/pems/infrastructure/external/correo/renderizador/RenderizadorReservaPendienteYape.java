package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.infrastructure.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorReservaPendienteYape implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;

    @Value("${playzone.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String tipoCodigo() {
        return "RESERVA_PENDIENTE_YAPE";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "numeroTicket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "fecha", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "",
                "total", reserva.getTotalPagado() != null ? reserva.getTotalPagado().toPlainString() : "0.00",
                "urlReserva", frontendUrl + "/cliente/mis-reservas?detalle=" + reserva.getId());

        String cuerpoHtml = templateService.procesarTemplate("email-reserva-pendiente-yape", variables);

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Reserva registrada — sube tu comprobante — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }
}
