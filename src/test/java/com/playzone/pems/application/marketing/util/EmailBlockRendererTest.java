package com.playzone.pems.application.marketing.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EmailBlockRendererTest {

    private EmailBlockRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new EmailBlockRenderer(new ObjectMapper());
    }

    @Test
    void testRenderizaParrafoConTextoPlano() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"paragraph\",\"texto\":\"Hola clientes\"}]";
        String resultado = renderer.renderizar(bloques, Map.of(), null);
        assertTrue(resultado.contains("Hola clientes"));
        assertTrue(resultado.contains("<p"));
    }

    @Test
    void testSustituyeVariablesConValorDelAdmin() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"paragraph\",\"texto\":\"Aprovecha {{promocion}} con {{descuento}} de descuento\"}]";
        String resultado = renderer.renderizar(bloques, Map.of("promocion", "Pack Verano", "descuento", "20%"), null);
        assertTrue(resultado.contains("Aprovecha Pack Verano con 20% de descuento"));
        assertFalse(resultado.contains("{{promocion}}"));
    }

    @Test
    void testEscapaHtmlEnTextoDelBloque() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"paragraph\",\"texto\":\"<script>alert(1)</script>\"}]";
        String resultado = renderer.renderizar(bloques, Map.of(), null);
        assertFalse(resultado.contains("<script>alert(1)</script>"));
        assertTrue(resultado.contains("&lt;script&gt;"));
    }

    @Test
    void testEscapaHtmlEnValorDeVariableSustituida() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"paragraph\",\"texto\":\"Hola {{nombreCliente}}\"}]";
        String resultado = renderer.renderizar(
                bloques, Map.of("nombreCliente", "<img src=x onerror=alert(1)>"), null);
        assertFalse(resultado.contains("<img src=x onerror=alert(1)>"));
        assertTrue(resultado.contains("&lt;img"));
    }

    @Test
    void testRenderizaBotonConTextoYUrl() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"button\",\"texto\":\"Ver oferta\",\"url\":\"https://kikiylala.lat/promo\"}]";
        String resultado = renderer.renderizar(bloques, Map.of(), null);
        assertTrue(resultado.contains("Ver oferta"));
        assertTrue(resultado.contains("https://kikiylala.lat/promo"));
        assertTrue(resultado.contains("<a href="));
    }

    @Test
    void testOmiteBotonSinUrl() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"button\",\"texto\":\"Ver oferta\"}]";
        String resultado = renderer.renderizar(bloques, Map.of(), null);
        assertFalse(resultado.contains("<a href"));
    }

    @Test
    void testIncluyeEnlaceDeBajaCuandoSeProporciona() {
        String resultado = renderer.renderizar("[]", Map.of(), "https://api.kikiylala.lat/unsubscribe?token=abc");
        assertTrue(resultado.contains("https://api.kikiylala.lat/unsubscribe?token=abc"));
        assertTrue(resultado.contains("darte de baja"));
    }

    @Test
    void testSinEnlaceDeBajaSiUrlEsNula() {
        String resultado = renderer.renderizar("[]", Map.of(), null);
        assertFalse(resultado.contains("darte de baja"));
    }

    @Test
    void testContenidoBloquesInvalidoNoLanzaExcepcion() {
        String resultado = renderer.renderizar("no-es-json-valido", Map.of(), null);
        assertNotNull(resultado);
    }

    @Test
    void testRenderizaDivider() {
        String bloques = "[{\"id\":\"1\",\"tipo\":\"divider\"}]";
        String resultado = renderer.renderizar(bloques, Map.of(), null);
        assertTrue(resultado.contains("<hr"));
    }
}
