package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.query.ChecklistEventoQuery;
import com.playzone.pems.application.evento.port.in.GestionarChecklistUseCase;
import com.playzone.pems.domain.evento.model.ChecklistEvento;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChecklistEventoService implements GestionarChecklistUseCase {

    private final ChecklistEventoRepository checklistRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistEventoQuery> listar(Long idEvento) {
        return checklistRepository.findByEventoOrdenado(idEvento)
                .stream().map(this::toQuery).toList();
    }

    @Override
    @Transactional
    public ChecklistEventoQuery completar(Long idEvento, Long idChecklist, UUID idUsuarioAdmin) {
        ChecklistEvento item = obtenerDelEvento(idEvento, idChecklist);

        if (item.isCompletada()) {
            throw new ValidationException("La tarea ya esta marcada como completada.");
        }

        return toQuery(checklistRepository.save(item.toBuilder()
                .completada(true)
                .idUsuarioCompleto(idUsuarioAdmin)
                .fechaCompletado(OffsetDateTime.now(ZoneId.of("America/Lima")))
                .build()));
    }

    @Override
    @Transactional
    public ChecklistEventoQuery descompletar(Long idEvento, Long idChecklist) {
        ChecklistEvento item = obtenerDelEvento(idEvento, idChecklist);

        return toQuery(checklistRepository.save(item.toBuilder()
                .completada(false)
                .idUsuarioCompleto(null)
                .fechaCompletado(null)
                .build()));
    }

    @Override
    @Transactional
    public ChecklistEventoQuery agregarTarea(Long idEvento, String tarea) {
        List<ChecklistEvento> existentes = checklistRepository.findByEventoOrdenado(idEvento);
        int siguienteOrden = existentes.stream()
                .mapToInt(ChecklistEvento::getOrden)
                .max()
                .orElse(0) + 1;

        ChecklistEvento nuevo = ChecklistEvento.builder()
                .idEventoPrivado(idEvento)
                .tarea(tarea.trim())
                .completada(false)
                .orden(siguienteOrden)
                .build();

        return toQuery(checklistRepository.save(nuevo));
    }

    @Override
    @Transactional
    public void eliminarTarea(Long idEvento, Long idChecklist) {
        obtenerDelEvento(idEvento, idChecklist);
        checklistRepository.deleteById(idChecklist);
    }

    private ChecklistEvento obtenerDelEvento(Long idEvento, Long idChecklist) {
        ChecklistEvento item = checklistRepository.findById(idChecklist)
                .orElseThrow(() -> new ResourceNotFoundException("ChecklistEvento", idChecklist));
        if (!item.getIdEventoPrivado().equals(idEvento)) {
            throw new ValidationException("La tarea de checklist no pertenece al evento indicado.");
        }
        return item;
    }

    private ChecklistEventoQuery toQuery(ChecklistEvento c) {
        return ChecklistEventoQuery.builder()
                .id(c.getId())
                .idEventoPrivado(c.getIdEventoPrivado())
                .tarea(c.getTarea())
                .completada(c.isCompletada())
                .orden(c.getOrden())
                .usuarioCompleto(c.getNombreUsuarioCompleto())
                .fechaCompletado(c.getFechaCompletado())
                .build();
    }
}