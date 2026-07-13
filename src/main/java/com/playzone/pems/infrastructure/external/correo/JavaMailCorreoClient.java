package com.playzone.pems.infrastructure.external.correo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.infrastructure.external.correo.renderizador.AdjuntoCorreo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JavaMailCorreoClient {

    private static final URI RESEND_ENDPOINT = URI.create("https://api.resend.com/emails");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${playzone.correo.remitente}")
    private String remitente;

    @Value("${playzone.correo.nombre-remitente:PlayZone}")
    private String nombreRemitente;

    @Value("${playzone.correo.api-key:}")
    private String apiKey;

    public void enviar(String destinatario, String asunto, String cuerpoHtml) {
        enviarInterno(destinatario, asunto, cuerpoHtml, List.of());
    }

    public void enviarConLogo(String destinatario, String asunto, String cuerpoHtml) {
        enviarInterno(destinatario, asunto, cuerpoHtml, List.of());
    }

    public void enviarConAdjuntos(String destinatario, String asunto, String cuerpoHtml, List<AdjuntoCorreo> adjuntos) {
        enviarInterno(destinatario, asunto, cuerpoHtml, adjuntos, nombreRemitente);
    }

    public void enviarConAdjuntos(String destinatario, String asunto, String cuerpoHtml, List<AdjuntoCorreo> adjuntos, String nombreRemitentePersonalizado) {
        enviarInterno(destinatario, asunto, cuerpoHtml, adjuntos, nombreRemitentePersonalizado);
    }

    private void enviarInterno(String destinatario, String asunto, String cuerpoHtml, List<AdjuntoCorreo> adjuntos) {
        enviarInterno(destinatario, asunto, cuerpoHtml, adjuntos, nombreRemitente);
    }

    private void enviarInterno(String destinatario, String asunto, String cuerpoHtml, List<AdjuntoCorreo> adjuntos, String nombreRemitenteEfectivo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from", nombreRemitenteEfectivo + " <" + remitente + ">");
        payload.put("to", List.of(destinatario));
        payload.put("subject", asunto);
        payload.put("html", cuerpoHtml);
        if (adjuntos != null && !adjuntos.isEmpty()) {
            payload.put("attachments", mapearAdjuntos(adjuntos));
        }

        try {
            String cuerpoJson = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(RESEND_ENDPOINT)
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(cuerpoJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new RuntimeException("Resend respondió " + response.statusCode() + ": " + response.body());
            }
            log.info("Correo enviado a {}: {}", destinatario, asunto);
        } catch (RuntimeException e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo electrónico.", e);
        }
    }

    private List<Map<String, String>> mapearAdjuntos(List<AdjuntoCorreo> adjuntos) {
        List<Map<String, String>> resultado = new ArrayList<>(adjuntos.size());
        for (AdjuntoCorreo adjunto : adjuntos) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("filename", adjunto.getNombreArchivo());
            item.put("content", Base64.getEncoder().encodeToString(adjunto.getContenido()));
            item.put("content_type", adjunto.getTipoContenido());
            resultado.add(item);
        }
        return resultado;
    }
}
