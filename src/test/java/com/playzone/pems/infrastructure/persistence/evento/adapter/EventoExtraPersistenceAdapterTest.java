package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoExtra;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ExtraPaqueteEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ExtraPaqueteJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoExtraJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoExtraPersistenceAdapterTest {

    @Mock private EventoExtraJpaRepository extraJpa;
    @Mock private EventoPrivadoJpaRepository eventoJpa;
    @Mock private ExtraPaqueteJpaRepository extraPaqueteJpa;

    private EventoExtraPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EventoExtraPersistenceAdapter(extraJpa, eventoJpa, extraPaqueteJpa);
    }

    @Test
    void testSaveAllResuelveElEventoUnaSolaVezParaVariosExtras() {
        EventoPrivadoEntity evento = EventoPrivadoEntity.builder().id(100L).build();
        when(eventoJpa.findAllById(any())).thenReturn(List.of(evento));
        when(extraPaqueteJpa.findAllById(any())).thenReturn(List.of(
                ExtraPaqueteEntity.builder().id(1L).build(),
                ExtraPaqueteEntity.builder().id(2L).build()));
        when(extraJpa.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<EventoExtra> extras = List.of(
                EventoExtra.builder().idEventoPrivado(100L).idExtra(1L).build(),
                EventoExtra.builder().idEventoPrivado(100L).idExtra(2L).build(),
                EventoExtra.builder().idEventoPrivado(100L).nombreLibre("Extra libre").build());

        List<EventoExtra> guardados = adapter.saveAll(extras);

        assertEquals(3, guardados.size());
        verify(eventoJpa, never()).findById(anyLong());
        verify(eventoJpa, times(1)).findAllById(any());
        verify(extraPaqueteJpa, never()).findById(anyLong());
        verify(extraPaqueteJpa, times(1)).findAllById(any());
    }

    @Test
    void testSaveAllConListaVaciaNoConsultaRepositorios() {
        List<EventoExtra> guardados = adapter.saveAll(List.of());

        assertEquals(0, guardados.size());
        verify(eventoJpa, never()).findAllById(any());
        verify(extraJpa, never()).saveAll(any());
    }

    @Test
    void testSaveAllPreservaCantidadYNotas() {
        EventoPrivadoEntity evento = EventoPrivadoEntity.builder().id(100L).build();
        when(eventoJpa.findAllById(any())).thenReturn(List.of(evento));
        when(extraJpa.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        List<EventoExtra> extras = List.of(
                EventoExtra.builder().idEventoPrivado(100L).nombreLibre("Globos")
                        .cantidad(3).notas("Color azul y blanco").build());

        List<EventoExtra> guardados = adapter.saveAll(extras);

        assertEquals(1, guardados.size());
        assertEquals(3, guardados.get(0).getCantidad());
        assertEquals("Color azul y blanco", guardados.get(0).getNotas());
    }
}
