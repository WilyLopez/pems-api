package com.playzone.pems.application.evento.port.in;

import com.playzone.pems.application.evento.dto.query.ChecklistEventoQuery;

import java.util.List;
import java.util.UUID;

public interface GestionarChecklistUseCase {

    List<ChecklistEventoQuery> listar(Long idEvento);

    default List<ChecklistEventoQuery> consultarPorEvento(Long idEvento) {
        return listar(idEvento);
    }

    ChecklistEventoQuery completar(Long idEvento, Long idChecklist, UUID idUsuarioAdmin);

    ChecklistEventoQuery descompletar(Long idEvento, Long idChecklist);

    ChecklistEventoQuery agregarTarea(Long idEvento, String tarea);

    void eliminarTarea(Long idEvento, Long idChecklist);
}
