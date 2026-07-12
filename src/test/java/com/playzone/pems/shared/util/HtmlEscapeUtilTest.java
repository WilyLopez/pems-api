package com.playzone.pems.shared.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlEscapeUtilTest {

    @Test
    void testEscaparEtiquetasScript() {
        String entrada = "<script>alert(1)</script>";
        String resultado = HtmlEscapeUtil.escapar(entrada);
        assertEquals("&lt;script&gt;alert(1)&lt;/script&gt;", resultado);
    }

    @Test
    void testEscaparAtributoOnerror() {
        String entrada = "<img src=x onerror=\"alert('xss')\">";
        String resultado = HtmlEscapeUtil.escapar(entrada);
        assertFalse(resultado.contains("<img"));
        assertTrue(resultado.contains("&lt;img"));
        assertTrue(resultado.contains("&quot;"));
        assertTrue(resultado.contains("&#39;"));
    }

    @Test
    void testEscaparAmpersand() {
        assertEquals("Juan &amp; Maria", HtmlEscapeUtil.escapar("Juan & Maria"));
    }

    @Test
    void testEscaparValorNulo() {
        assertEquals("", HtmlEscapeUtil.escapar(null));
    }

    @Test
    void testEscaparTextoSinCaracteresEspeciales() {
        assertEquals("Juan Perez", HtmlEscapeUtil.escapar("Juan Perez"));
    }
}
