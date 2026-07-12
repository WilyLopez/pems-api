package com.playzone.pems.infrastructure.template;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TemplateServiceTest {

    private final TemplateService templateService = new TemplateService();

    @Test
    void testProcesarTemplateEscapaValorConHtml() {
        String resultado = templateService.procesarTemplate("welcome-user", Map.of(
                "nombre", "<script>alert(1)</script>",
                "correo", "cliente@correo.com",
                "password", "clave123",
                "rol", "ADMIN",
                "sede", "Sede Principal",
                "loginUrl", "http://localhost:3000/auth/login"
        ));

        assertFalse(resultado.contains("<script>alert(1)</script>"));
        assertTrue(resultado.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
    }

    @Test
    void testProcesarTemplateSustituyeVariablesSinCaracteresEspeciales() {
        String resultado = templateService.procesarTemplate("welcome-user", Map.of(
                "nombre", "Juan Perez",
                "correo", "juan@correo.com",
                "password", "clave123",
                "rol", "CAJERO",
                "sede", "Sede Norte",
                "loginUrl", "http://localhost:3000/auth/login"
        ));

        assertTrue(resultado.contains("Juan Perez"));
        assertTrue(resultado.contains("juan@correo.com"));
        assertTrue(resultado.contains("CAJERO"));
    }

    @Test
    void testProcesarTemplatePlantillaInexistenteLanzaExcepcion() {
        assertThrows(RuntimeException.class, () ->
                templateService.procesarTemplate("plantilla-que-no-existe", Map.of()));
    }
}
