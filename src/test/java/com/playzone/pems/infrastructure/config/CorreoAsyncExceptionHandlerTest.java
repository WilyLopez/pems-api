package com.playzone.pems.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CorreoAsyncExceptionHandlerTest {

    private final CorreoAsyncExceptionHandler handler = new CorreoAsyncExceptionHandler();

    @Test
    void testHandleUncaughtExceptionNoRelanzaLaExcepcion() throws NoSuchMethodException {
        Method metodoDeEjemplo = String.class.getMethod("toString");
        RuntimeException fallo = new RuntimeException("fallo simulado de SMTP");

        assertDoesNotThrow(() ->
                handler.handleUncaughtException(fallo, metodoDeEjemplo, "argumento1", 42));
    }
}
