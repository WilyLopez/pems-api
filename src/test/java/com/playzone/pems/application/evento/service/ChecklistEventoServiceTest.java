package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.query.ChecklistEventoQuery;
import com.playzone.pems.domain.evento.model.ChecklistEvento;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChecklistEventoServiceTest {

    @Mock private ChecklistEventoRepository checklistRepository;
    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private SedeScopeValidator sedeScope;

    private ChecklistEventoService service;

    @BeforeEach
    void setUp() {
        service = new ChecklistEventoService(checklistRepository, eventoRepository, sedeScope);
    }

    private EventoPrivado eventoPrueba(Long id, Long idSede) {
        return EventoPrivado.builder()
                .id(id).idCliente(5L).idSede(idSede).idTurno(1L)
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .build();
    }

    private ChecklistEvento checklistPrueba(Long id, Long idEvento) {
        return ChecklistEvento.builder()
                .id(id).idEventoPrivado(idEvento).tarea("Decoracion lista")
                .completada(false).orden(1).build();
    }

    @Test
    void testListarConAccesoDenegadoPorSedeLanzaAccessDeniedException() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 2L)));
        doThrow(new AccessDeniedException("Sin acceso a la sede."))
                .when(sedeScope).validarAcceso(2L);

        assertThrows(AccessDeniedException.class, () -> service.listar(100L));
    }

    @Test
    void testListarConAccesoPermitidoDevuelveTareas() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 1L)));
        when(checklistRepository.findByEventoOrdenado(100L))
                .thenReturn(List.of(checklistPrueba(1L, 100L)));

        List<ChecklistEventoQuery> resultado = service.listar(100L);

        assertEquals(1, resultado.size());
        verify(sedeScope).validarAcceso(1L);
    }

    @Test
    void testListarConEventoInexistenteLanzaResourceNotFoundException() {
        when(eventoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listar(999L));
    }

    @Test
    void testCompletarConAccesoDenegadoPorSedeLanzaAccessDeniedException() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 2L)));
        doThrow(new AccessDeniedException("Sin acceso a la sede."))
                .when(sedeScope).validarAcceso(2L);

        assertThrows(AccessDeniedException.class,
                () -> service.completar(100L, 1L, UUID.randomUUID()));
        verify(checklistRepository, never()).findById(anyLong());
    }

    @Test
    void testDescompletarConAccesoDenegadoPorSedeLanzaAccessDeniedException() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 2L)));
        doThrow(new AccessDeniedException("Sin acceso a la sede."))
                .when(sedeScope).validarAcceso(2L);

        assertThrows(AccessDeniedException.class, () -> service.descompletar(100L, 1L));
    }

    @Test
    void testAgregarTareaConAccesoDenegadoPorSedeLanzaAccessDeniedException() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 2L)));
        doThrow(new AccessDeniedException("Sin acceso a la sede."))
                .when(sedeScope).validarAcceso(2L);

        assertThrows(AccessDeniedException.class, () -> service.agregarTarea(100L, "Nueva tarea"));
    }

    @Test
    void testEliminarTareaConAccesoDenegadoPorSedeLanzaAccessDeniedException() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 2L)));
        doThrow(new AccessDeniedException("Sin acceso a la sede."))
                .when(sedeScope).validarAcceso(2L);

        assertThrows(AccessDeniedException.class, () -> service.eliminarTarea(100L, 1L));
    }

    @Test
    void testCompletarTareaYaCompletadaLanzaValidationException() {
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(eventoPrueba(100L, 1L)));
        ChecklistEvento item = checklistPrueba(1L, 100L).toBuilder().completada(true).build();
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> service.completar(100L, 1L, UUID.randomUUID()));
    }

    @Test
    void testCompletarLaUltimaTareaPendienteMarcaElChecklistCompletoEnElEvento() {
        EventoPrivado evento = eventoPrueba(100L, 1L).toBuilder().checklistCompleto(false).build();
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(evento));
        ChecklistEvento item = checklistPrueba(1L, 100L);
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(item));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ChecklistEvento otraYaCompletada = checklistPrueba(2L, 100L).toBuilder().completada(true).build();
        when(checklistRepository.findByEventoOrdenado(100L))
                .thenReturn(List.of(item.toBuilder().completada(true).build(), otraYaCompletada));

        service.completar(100L, 1L, UUID.randomUUID());

        ArgumentCaptor<EventoPrivado> captor = ArgumentCaptor.forClass(EventoPrivado.class);
        verify(eventoRepository).save(captor.capture());
        assertTrue(captor.getValue().isChecklistCompleto());
    }

    @Test
    void testCompletarUnaTareaConOtrasPendientesNoMarcaElChecklistCompleto() {
        EventoPrivado evento = eventoPrueba(100L, 1L).toBuilder().checklistCompleto(false).build();
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(evento));
        ChecklistEvento item = checklistPrueba(1L, 100L);
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(item));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        ChecklistEvento otraPendiente = checklistPrueba(2L, 100L);
        when(checklistRepository.findByEventoOrdenado(100L))
                .thenReturn(List.of(item.toBuilder().completada(true).build(), otraPendiente));

        service.completar(100L, 1L, UUID.randomUUID());

        verify(eventoRepository, never()).save(any());
    }

    @Test
    void testDescompletarUnaTareaDesmarcaElChecklistCompletoEnElEvento() {
        EventoPrivado evento = eventoPrueba(100L, 1L).toBuilder().checklistCompleto(true).build();
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(evento));
        ChecklistEvento item = checklistPrueba(1L, 100L).toBuilder().completada(true).build();
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(item));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(checklistRepository.findByEventoOrdenado(100L))
                .thenReturn(List.of(item.toBuilder().completada(false).build()));

        service.descompletar(100L, 1L);

        ArgumentCaptor<EventoPrivado> captor = ArgumentCaptor.forClass(EventoPrivado.class);
        verify(eventoRepository).save(captor.capture());
        assertFalse(captor.getValue().isChecklistCompleto());
    }

    @Test
    void testAgregarTareaConChecklistPreviamenteCompletoLoMarcaComoIncompleto() {
        EventoPrivado evento = eventoPrueba(100L, 1L).toBuilder().checklistCompleto(true).build();
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(evento));
        ChecklistEvento existente = checklistPrueba(1L, 100L).toBuilder().completada(true).build();
        when(checklistRepository.findByEventoOrdenado(100L))
                .thenReturn(List.of(existente))
                .thenReturn(List.of(existente, checklistPrueba(2L, 100L)));
        when(checklistRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.agregarTarea(100L, "Nueva tarea");

        ArgumentCaptor<EventoPrivado> captor = ArgumentCaptor.forClass(EventoPrivado.class);
        verify(eventoRepository).save(captor.capture());
        assertFalse(captor.getValue().isChecklistCompleto());
    }

    @Test
    void testEliminarLaUltimaTareaPendienteCompletaElChecklist() {
        EventoPrivado evento = eventoPrueba(100L, 1L).toBuilder().checklistCompleto(false).build();
        when(eventoRepository.findById(100L)).thenReturn(Optional.of(evento));
        ChecklistEvento pendiente = checklistPrueba(1L, 100L);
        ChecklistEvento yaCompletada = checklistPrueba(2L, 100L).toBuilder().completada(true).build();
        when(checklistRepository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(checklistRepository.findByEventoOrdenado(100L)).thenReturn(List.of(yaCompletada));

        service.eliminarTarea(100L, 1L);

        ArgumentCaptor<EventoPrivado> captor = ArgumentCaptor.forClass(EventoPrivado.class);
        verify(eventoRepository).save(captor.capture());
        assertTrue(captor.getValue().isChecklistCompleto());
    }
}
