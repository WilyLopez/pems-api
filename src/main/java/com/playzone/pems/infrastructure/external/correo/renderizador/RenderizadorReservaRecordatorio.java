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
public class RenderizadorReservaRecordatorio implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "RESERVA_RECORDATORIO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "sede", reserva.getNombreSede() != null ? reserva.getNombreSede() : "",
                "fecha", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "",
                "ticket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "");

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Te esperamos mañana — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-reserva-recordatorio", variables))
                .build();
    }
}
