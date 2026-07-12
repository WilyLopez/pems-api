package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.infrastructure.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorReservaReprogramada implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public String tipoCodigo() {
        return "RESERVA_REPROGRAMADA";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "sede", reserva.getNombreSede() != null ? reserva.getNombreSede() : "",
                "ticket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "fechaAnterior", extraerFechaAnterior(notificacion.getMetadata()),
                "fechaNueva", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "");

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Tu reserva cambió de fecha — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-reserva-reprogramada", variables))
                .build();
    }

    private String extraerFechaAnterior(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return "";
        try {
            JsonNode nodo = objectMapper.readTree(metadataJson);
            String fecha = nodo.path("fechaAnterior").asText(null);
            return fecha != null ? fecha : "";
        } catch (Exception e) {
            return "";
        }
    }
}
