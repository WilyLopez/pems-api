package com.playzone.pems.domain.evento.repository;

import com.playzone.pems.domain.evento.model.ChecklistEvento;

import java.util.List;
import java.util.Optional;

public interface ChecklistEventoRepository {
    Optional<ChecklistEvento> findById(Long id);
    List<ChecklistEvento> findByEventoOrdenado(Long idEvento);
    ChecklistEvento save(ChecklistEvento checklist);
    void crearTareasBase(Long idEvento, List<String> tareas);
    void deleteById(Long id);
}