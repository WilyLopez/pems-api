package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.ChecklistEvento;
import com.playzone.pems.infrastructure.persistence.evento.entity.ChecklistEventoEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.ChecklistEventoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.mapper.ChecklistEventoEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.PerfilUsuarioEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.PerfilUsuarioJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChecklistEventoPersistenceAdapterTest {

    @Mock private ChecklistEventoJpaRepository checklistJpa;
    @Mock private EventoPrivadoJpaRepository eventoJpa;
    @Mock private PerfilUsuarioJpaRepository perfilJpa;
    @Mock private ChecklistEventoEntityMapper mapper;

    private ChecklistEventoPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ChecklistEventoPersistenceAdapter(checklistJpa, eventoJpa, perfilJpa, mapper);
    }

    private ChecklistEventoEntity entidad(Long id, UUID idUsuarioCompleto) {
        return ChecklistEventoEntity.builder().id(id).tarea("Tarea " + id).completadaPor(idUsuarioCompleto).build();
    }

    private ChecklistEvento dominio(Long id, UUID idUsuarioCompleto) {
        return ChecklistEvento.builder().id(id).idEventoPrivado(100L).tarea("Tarea " + id)
                .idUsuarioCompleto(idUsuarioCompleto).build();
    }

    private PerfilUsuarioEntity perfil(UUID id, String nombreCompleto) {
        PerfilUsuarioEntity p = new PerfilUsuarioEntity();
        ReflectionTestUtils.setField(p, "id", id);
        ReflectionTestUtils.setField(p, "nombreCompleto", nombreCompleto);
        return p;
    }

    @Test
    void testFindByEventoOrdenadoResuelveNombresDeUsuarioEnUnSoloBatch() {
        UUID usuario1 = UUID.randomUUID();
        UUID usuario2 = UUID.randomUUID();

        ChecklistEventoEntity entidad1 = entidad(1L, usuario1);
        ChecklistEventoEntity entidad2 = entidad(2L, usuario2);
        ChecklistEventoEntity entidad3 = entidad(3L, null);
        when(checklistJpa.findByEventoPrivado_IdOrderByOrdenAsc(100L))
                .thenReturn(List.of(entidad1, entidad2, entidad3));

        when(mapper.toDomain(entidad1)).thenReturn(dominio(1L, usuario1));
        when(mapper.toDomain(entidad2)).thenReturn(dominio(2L, usuario2));
        when(mapper.toDomain(entidad3)).thenReturn(dominio(3L, null));

        when(perfilJpa.findAllById(any())).thenReturn(List.of(
                perfil(usuario1, "Ana"),
                perfil(usuario2, "Luis")));

        List<ChecklistEvento> resultado = adapter.findByEventoOrdenado(100L);

        assertEquals(3, resultado.size());
        assertEquals("Ana", resultado.get(0).getNombreUsuarioCompleto());
        assertEquals("Luis", resultado.get(1).getNombreUsuarioCompleto());
        verify(perfilJpa, never()).findByIdAndDeletedAtIsNull(any());
        verify(perfilJpa, times(1)).findAllById(any());
    }

    @Test
    void testFindByEventoOrdenadoConListaVaciaNoConsultaPerfiles() {
        when(checklistJpa.findByEventoPrivado_IdOrderByOrdenAsc(100L)).thenReturn(List.of());

        List<ChecklistEvento> resultado = adapter.findByEventoOrdenado(100L);

        assertEquals(0, resultado.size());
        verify(perfilJpa, never()).findAllById(any());
    }

    @Test
    void testCrearTareasBaseUsaSaveAllEnVezDeSaveEnLoop() {
        EventoPrivadoEntity evento = EventoPrivadoEntity.builder().id(100L).build();
        when(eventoJpa.findById(100L)).thenReturn(Optional.of(evento));
        when(checklistJpa.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        adapter.crearTareasBase(100L, List.of("Tarea A", "Tarea B", "Tarea C"));

        verify(checklistJpa, never()).save(any());
        ArgumentCaptor<List<ChecklistEventoEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(checklistJpa, times(1)).saveAll(captor.capture());
        assertEquals(3, captor.getValue().size());
        assertEquals("Tarea A", captor.getValue().get(0).getTarea());
    }
}
