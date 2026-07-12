package com.playzone.pems.shared.ratelimit;

import com.playzone.pems.interfaces.rest.cms.MensajeContactoController;
import com.playzone.pems.interfaces.rest.evento.EventoPrivadoController;
import com.playzone.pems.interfaces.rest.evento.ReservaPublicaController;
import com.playzone.pems.interfaces.rest.evento.request.CrearReservaRequest;
import com.playzone.pems.interfaces.rest.evento.request.SolicitarEventoPrivadoRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitedEndpointsTest {

    @Test
    void testCrearReservaPublicaTieneRateLimited() throws NoSuchMethodException {
        Method metodo = ReservaPublicaController.class.getMethod(
                "crear", Long.class, Long.class, CrearReservaRequest.class);
        assertTrue(metodo.isAnnotationPresent(RateLimited.class));
    }

    @Test
    void testCrearConParamsReservaPublicaTieneRateLimited() throws NoSuchMethodException {
        Method metodo = ReservaPublicaController.class.getMethod(
                "crearConParams", Long.class, Long.class, CrearReservaRequest.class);
        assertTrue(metodo.isAnnotationPresent(RateLimited.class));
    }

    @Test
    void testSolicitarEventoPrivadoTieneRateLimited() throws NoSuchMethodException {
        Method metodo = EventoPrivadoController.class.getMethod(
                "solicitar", Long.class, Long.class, SolicitarEventoPrivadoRequest.class);
        assertTrue(metodo.isAnnotationPresent(RateLimited.class));
    }

    @Test
    void testRegistrarMensajeContactoTieneRateLimitedMasEstricto() throws NoSuchMethodException {
        Method metodo = MensajeContactoController.class.getMethod(
                "registrar",
                MensajeContactoController.RegistrarMensajeRequest.class,
                HttpServletRequest.class);

        assertTrue(metodo.isAnnotationPresent(RateLimited.class));
        RateLimited anotacion = metodo.getAnnotation(RateLimited.class);
        assertEquals(3, anotacion.requests());
        assertEquals(60, anotacion.durationInSeconds());
    }
}
