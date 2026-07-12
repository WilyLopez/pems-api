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
public class RenderizadorReservaCancelada implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "RESERVA_CANCELADA";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "numeroTicket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "fecha", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "",
                "motivo", reserva.getMotivoCancelacion() != null ? reserva.getMotivoCancelacion() : "No especificado");

        String cuerpoHtml = templateService.procesarTemplate("email-reserva-cancelada", variables);

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Reserva cancelada — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }
}
