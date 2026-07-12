package com.playzone.pems.application.marketing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.marketing.dto.EmailBlockDto;
import com.playzone.pems.shared.util.HtmlEscapeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EmailBlockRenderer {

    private final ObjectMapper objectMapper;

    public String renderizar(String contenidoBloques, Map<String, String> valoresVariables, String urlBaja) {
        StringBuilder cuerpo = new StringBuilder();
        for (EmailBlockDto bloque : parsearBloques(contenidoBloques)) {
            cuerpo.append(renderizarBloque(bloque, valoresVariables));
        }
        cuerpo.append(piePagina(urlBaja));
        return envolver(cuerpo.toString());
    }

    private List<EmailBlockDto> parsearBloques(String contenidoBloques) {
        if (contenidoBloques == null || contenidoBloques.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(contenidoBloques,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, EmailBlockDto.class));
        } catch (Exception e) {
            return List.of();
        }
    }

    private String renderizarBloque(EmailBlockDto bloque, Map<String, String> valores) {
        String tipo = bloque.getTipo() != null ? bloque.getTipo() : "";
        String texto = sustituirVariables(bloque.getTexto(), valores);
        String url = sustituirVariables(bloque.getUrl(), valores);
        String alt = sustituirVariables(bloque.getAlt(), valores);
        boolean nivel2 = bloque.getNivel() != null && bloque.getNivel() == 2;

        return switch (tipo) {
            case "heading" -> encabezado(texto, nivel2);
            case "paragraph" -> texto.isEmpty() ? "" : "<p style=\"color:#334155;line-height:1.6;margin:12px 0;font-size:14px;\">" + texto + "</p>";
            case "image" -> url.isEmpty() ? "" : "<img src=\"" + url + "\" alt=\"" + alt + "\" style=\"max-width:100%;border-radius:8px;margin:12px 0;\"/>";
            case "button" -> boton(texto, url);
            case "divider" -> "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:20px 0;\"/>";
            default -> "";
        };
    }

    private String encabezado(String texto, boolean nivel2) {
        if (texto.isEmpty()) {
            return "";
        }
        String etiqueta = nivel2 ? "h2" : "h1";
        String tamano = nivel2 ? "18px" : "22px";
        return "<" + etiqueta + " style=\"color:#1A1A2E;margin:16px 0;font-size:" + tamano + ";\">" + texto + "</" + etiqueta + ">";
    }

    private String boton(String texto, String url) {
        if (texto.isEmpty() || url.isEmpty()) {
            return "";
        }
        return "<div style=\"text-align:center;margin:20px 0;\">"
                + "<a href=\"" + url + "\" style=\"background:#00AEEF;color:#ffffff;padding:12px 24px;"
                + "border-radius:24px;text-decoration:none;font-weight:bold;display:inline-block;\">"
                + texto + "</a></div>";
    }

    private String sustituirVariables(String texto, Map<String, String> valores) {
        if (texto == null) {
            return "";
        }
        String resultado = HtmlEscapeUtil.escapar(texto);
        for (Map.Entry<String, String> entrada : valores.entrySet()) {
            resultado = resultado.replace(
                    "{{" + entrada.getKey() + "}}",
                    HtmlEscapeUtil.escapar(entrada.getValue()));
        }
        return resultado;
    }

    private String piePagina(String urlBaja) {
        if (urlBaja == null || urlBaja.isBlank()) {
            return "";
        }
        return "<p style=\"color:#94a3b8;font-size:11px;margin-top:24px;text-align:center;\">"
                + "Si no deseas volver a recibir estos correos, "
                + "<a href=\"" + urlBaja + "\" style=\"color:#94a3b8;text-decoration:underline;\">haz clic aquí para darte de baja</a>."
                + "</p>";
    }

    private String envolver(String contenido) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:0 auto;\">"
                + "<div style=\"background:#f8fafc;padding:24px;border:1px solid #e2e8f0;border-radius:12px;\">"
                + contenido
                + "</div></div>";
    }
}
