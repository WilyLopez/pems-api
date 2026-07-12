package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.infrastructure.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorCambioCorreoSolicitado implements RenderizadorCorreoTransaccional {

    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Value("${playzone.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String tipoCodigo() {
        return "CAMBIO_CORREO_SOLICITADO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        JsonNode nodo = leerMetadata(notificacion.getMetadata());
        String correoNuevo = requerido(nodo, "correoNuevo");
        String tokenCorreo = requerido(nodo, "tokenCorreo");

        Map<String, String> variables = Map.of(
                "correoNuevo", correoNuevo,
                "confirmacionUrl", frontendUrl + "/cliente/mi-cuenta/confirmar-correo?token=" + tokenCorreo);

        return ContenidoCorreo.builder()
                .destinatario(correoNuevo)
                .asunto("Confirma tu nuevo correo — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-cambio-correo-solicitado", variables))
                .build();
    }

    private JsonNode leerMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            throw new IllegalStateException("La notificación de cambio de correo no contiene metadata.");
        }
        try {
            return objectMapper.readTree(metadataJson);
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer la metadata de cambio de correo.", e);
        }
    }

    private String requerido(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asText(null);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException("La notificación de cambio de correo no contiene '" + campo + "'.");
        }
        return valor;
    }
}
