package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoServicio;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioCotizacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoServicioEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoServicioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoServicioPersistenceAdapterTest {

    @Mock private EventoServicioJpaRepository servicioJpa;
    @Mock private EventoPrivadoJpaRepository eventoJpa;
    @Mock private ServicioCotizacionJpaRepository servicioCotizacionJpa;

    private EventoServicioPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EventoServicioPersistenceAdapter(servicioJpa, eventoJpa, servicioCotizacionJpa);
    }

    @Test
    void testSaveAllResuelveElEventoUnaSolaVezParaVariosServicios() {
        EventoPrivadoEntity evento = EventoPrivadoEntity.builder().id(100L).build();
        when(eventoJpa.findAllById(any())).thenReturn(List.of(evento));
        when(servicioCotizacionJpa.findAllById(any())).thenReturn(List.of(
                ServicioCotizacionEntity.builder().id(10L).build(),
                ServicioCotizacionEntity.builder().id(20L).build()));
        when(servicioJpa.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<EventoServicio> servicios = List.of(
                EventoServicio.builder().idEventoPrivado(100L).idServicioCotizacion(10L)
                        .nombreLibre("Torta").precioAcordado(new BigDecimal("50")).incluido(true).build(),
                EventoServicio.builder().idEventoPrivado(100L).idServicioCotizacion(20L)
                        .nombreLibre("Show").precioAcordado(new BigDecimal("150")).incluido(true).build(),
                EventoServicio.builder().idEventoPrivado(100L).nombreLibre("Extra libre")
                        .precioAcordado(BigDecimal.ZERO).incluido(true).build());

        List<EventoServicio> guardados = adapter.saveAll(servicios);

        assertEquals(3, guardados.size());
        verify(eventoJpa, never()).findById(anyLong());
        verify(eventoJpa, times(1)).findAllById(any());
        verify(servicioCotizacionJpa, never()).findById(anyLong());
        verify(servicioCotizacionJpa, times(1)).findAllById(any());
    }

    @Test
    void testSaveAllConListaVaciaNoConsultaRepositorios() {
        List<EventoServicio> guardados = adapter.saveAll(List.of());

        assertEquals(0, guardados.size());
        verify(eventoJpa, never()).findAllById(any());
        verify(servicioJpa, never()).saveAll(any());
    }
}
