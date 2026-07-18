package com.playzone.pems.shared.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class HttpRequestUtilsTest {

    @AfterEach
    void limpiarContexto() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void ipActual_prefiereXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.5, 10.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("203.0.113.5", HttpRequestUtils.ipActual());
    }

    @Test
    void ipActual_usaRemoteAddrSiNoHayHeaders() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("192.168.1.10", HttpRequestUtils.ipActual());
    }

    @Test
    void ipActual_devuelveNullSinContextoDeRequest() {
        assertNull(HttpRequestUtils.ipActual());
    }

    @Test
    void userAgentActual_retornaHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "Mozilla/5.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertEquals("Mozilla/5.0", HttpRequestUtils.userAgentActual());
    }

    @Test
    void userAgentActual_devuelveNullSinContextoDeRequest() {
        assertNull(HttpRequestUtils.userAgentActual());
    }
}
