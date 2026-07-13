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
public class RenderizadorReservaReprogramadaConPago implements RenderizadorCorreoTransaccional {

    private final ConsultarReservasUseCase consultarReservasUseCase;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Value("${playzone.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String tipoCodigo() {
        return "RESERVA_REPROGRAMADA_CON_PAGO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ReservaPublicaQuery reserva = consultarReservasUseCase.consultarPorId(notificacion.getEntidadId());

        Map<String, String> variables = Map.of(
                "nombreCliente", reserva.getNombreCliente() != null ? reserva.getNombreCliente() : "",
                "ticket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : "",
                "fechaAnterior", extraerMetadata(notificacion.getMetadata(), "fechaAnterior"),
                "fechaNueva", reserva.getFechaEvento() != null ? reserva.getFechaEvento().toString() : "",
                "montoAdicional", extraerMetadata(notificacion.getMetadata(), "montoAdicional"),
                "urlReserva", frontendUrl + "/cliente/mis-reservas/" + reserva.getId());

        return ContenidoCorreo.builder()
                .destinatario(reserva.getCorreoCliente())
                .asunto("Tu reserva cambió de fecha — falta un pago — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-reserva-reprogramada-con-pago", variables))
                .build();
    }

    private String extraerMetadata(String metadataJson, String campo) {
        if (metadataJson == null || metadataJson.isBlank()) return "";
        try {
            JsonNode nodo = objectMapper.readTree(metadataJson);
            String valor = nodo.path(campo).asText(null);
            return valor != null ? valor : "";
        } catch (Exception e) {
            return "";
        }
    }
}
