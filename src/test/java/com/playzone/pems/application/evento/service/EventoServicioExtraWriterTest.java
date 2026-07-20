package com.playzone.pems.application.evento.service;

import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.domain.evento.model.EventoServicio;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServicioExtraWriterTest {

    @Mock private ServicioCotizacionRepository servicioCotizacionRepository;
    @Mock private ServicioVarianteRepository    servicioVarianteRepository;
    @Mock private EventoServicioRepository      eventoServicioRepository;
    @Mock private EventoExtraRepository         eventoExtraRepository;

    private EventoServicioExtraWriter crearWriter() {
        return new EventoServicioExtraWriter(
                servicioCotizacionRepository, servicioVarianteRepository,
                eventoServicioRepository, eventoExtraRepository);
    }

    private ServicioCotizacion servicioSinVariantes() {
        return ServicioCotizacion.builder()
                .id(1L).nombre("Show de magia").activo(true)
                .precioReferencial(new BigDecimal("150.00"))
                .build();
    }

    private ServicioCotizacion servicioConVariantes() {
        return ServicioCotizacion.builder()
                .id(2L).nombre("Torta").activo(true)
                .precioReferencial(new BigDecimal("100.00"))
                .build();
    }

    @Test
    void testServicioSinVariantesSePersisteConPrecioReferencial() {
        EventoServicioExtraWriter writer = crearWriter();
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(servicioSinVariantes()));
        when(servicioVarianteRepository.findByServicio(1L)).thenReturn(List.of());

        writer.persistirServiciosCotizacion(99L, List.of(1L), Map.of());

        ArgumentCaptor<List<EventoServicio>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventoServicioRepository).saveAll(captor.capture());
        EventoServicio guardado = captor.getValue().get(0);
        assertEquals("Show de magia", guardado.getNombreLibre());
        assertEquals(new BigDecimal("150.00"), guardado.getPrecioAcordado());
        assertNull(guardado.getIdServicioVariante());
    }

    @Test
    void testServicioConVariantesSinSeleccionEsRechazado() {
        EventoServicioExtraWriter writer = crearWriter();
        ServicioVariante variante = ServicioVariante.builder()
                .id(10L).idServicio(2L).nombre("Chocolate").precio(new BigDecimal("120.00")).activo(true)
                .build();
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(servicioConVariantes()));
        when(servicioVarianteRepository.findByServicio(2L)).thenReturn(List.of(variante));

        assertThrows(ValidationException.class,
                () -> writer.persistirServiciosCotizacion(99L, List.of(2L), Map.of()));

        verify(eventoServicioRepository, never()).saveAll(anyList());
    }

    @Test
    void testServicioConVarianteInactivaSeleccionadaEsRechazado() {
        EventoServicioExtraWriter writer = crearWriter();
        ServicioVariante activa = ServicioVariante.builder()
                .id(10L).idServicio(2L).nombre("Chocolate").precio(new BigDecimal("120.00")).activo(true)
                .build();
        ServicioVariante inactiva = ServicioVariante.builder()
                .id(11L).idServicio(2L).nombre("Vainilla").precio(new BigDecimal("110.00")).activo(false)
                .build();
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(servicioConVariantes()));
        when(servicioVarianteRepository.findByServicio(2L)).thenReturn(List.of(activa, inactiva));

        assertThrows(ValidationException.class,
                () -> writer.persistirServiciosCotizacion(99L, List.of(2L), Map.of(2L, 11L)));
    }

    @Test
    void testServicioConVarianteSeleccionadaSePersisteConPrecioDeVariante() {
        EventoServicioExtraWriter writer = crearWriter();
        ServicioVariante variante = ServicioVariante.builder()
                .id(10L).idServicio(2L).nombre("Chocolate").precio(new BigDecimal("120.00")).activo(true)
                .build();
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(servicioConVariantes()));
        when(servicioVarianteRepository.findByServicio(2L)).thenReturn(List.of(variante));

        assertDoesNotThrow(() ->
                writer.persistirServiciosCotizacion(99L, List.of(2L), Map.of(2L, 10L)));

        ArgumentCaptor<List<EventoServicio>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventoServicioRepository).saveAll(captor.capture());
        EventoServicio guardado = captor.getValue().get(0);
        assertEquals(10L, guardado.getIdServicioVariante());
        assertEquals("Torta - Chocolate", guardado.getNombreLibre());
        assertEquals(new BigDecimal("120.00"), guardado.getPrecioAcordado());
    }

    @Test
    void testSinServiciosNoPersisteNada() {
        EventoServicioExtraWriter writer = crearWriter();

        writer.persistirServiciosCotizacion(99L, List.of(), Map.of());

        verify(eventoServicioRepository, never()).saveAll(anyList());
    }
}
