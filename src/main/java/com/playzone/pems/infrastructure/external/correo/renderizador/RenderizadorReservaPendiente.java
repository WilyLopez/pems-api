package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.infrastructure.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorReservaPendiente implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "TICKET_DISPONIBLE";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "numeroTicket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "fecha", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "",
                "total", reserva.getTotalPagado() != null ? reserva.getTotalPagado().toPlainString() : "0.00");

        String cuerpoHtml = templateService.procesarTemplate("email-reserva-pendiente", variables);

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Reserva pendiente de confirmación — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }
}
