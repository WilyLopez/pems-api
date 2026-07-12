package com.playzone.pems.infrastructure.template;

import com.playzone.pems.shared.util.HtmlEscapeUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Servicio para la gestión de plantillas HTML de correos.
 */
@Slf4j
@Service
public class TemplateService {

    /**
     * Carga una plantilla HTML desde resources/templates y reemplaza las variables dinámicas.
     *
     * @param templateName Nombre del archivo (sin extensión .html)
     * @param variables    Mapa de variables {{key}} -> valor
     * @return HTML procesado
     */
    public String procesarTemplate(String templateName, Map<String, String> variables) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + templateName + ".html");
            if (!resource.exists()) {
                throw new RuntimeException("La plantilla " + templateName + ".html no existe en resources/templates");
            }
            
            String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "{{" + entry.getKey() + "}}";
                content = content.replace(placeholder, HtmlEscapeUtil.escapar(entry.getValue()));
            }

            return content;
        } catch (IOException e) {
            log.error("Error al cargar el template {}: {}", templateName, e.getMessage());
            throw new RuntimeException("No se pudo cargar la plantilla de correo: " + templateName, e);
        }
    }
}
