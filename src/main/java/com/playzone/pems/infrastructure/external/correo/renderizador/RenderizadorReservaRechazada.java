package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RenderizadorReservaRechazada implements RenderizadorCorreoTransaccional {

    private static final String MOTIVO_DEFAULT = "Comprobante inválido";

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Value("${playzone.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String tipoCodigo() {
        return "PAGO_RECHAZADO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "numeroTicket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "motivo", extraerMotivo(notificacion.getMetadata()),
                "urlReserva", frontendUrl + "/cliente/mis-reservas?detalle=" + reserva.getId());

        String cuerpoHtml = templateService.procesarTemplate("email-reserva-rechazada", variables);

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Reserva rechazada — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }

    private String extraerMotivo(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return MOTIVO_DEFAULT;
        }
        try {
            JsonNode nodo = objectMapper.readTree(metadataJson);
            String motivo = nodo.path("motivo").asText(null);
            return motivo != null && !motivo.isBlank() ? motivo : MOTIVO_DEFAULT;
        } catch (Exception e) {
            return MOTIVO_DEFAULT;
        }
    }
}
