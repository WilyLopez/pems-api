package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.ChecklistEvento;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.ChecklistEventoEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.ChecklistEventoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.mapper.ChecklistEventoEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.PerfilUsuarioEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.PerfilUsuarioJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ChecklistEventoPersistenceAdapter implements ChecklistEventoRepository {

    private final ChecklistEventoJpaRepository checklistJpa;
    private final EventoPrivadoJpaRepository   eventoJpa;
    private final PerfilUsuarioJpaRepository   perfilJpa;
    private final ChecklistEventoEntityMapper  mapper;

    @Override
    public Optional<ChecklistEvento> findById(Long id) {
        return checklistJpa.findById(id).map(mapper::toDomain).map(this::enriquecer);
    }

    @Override
    public List<ChecklistEvento> findByEventoOrdenado(Long idEvento) {
        List<ChecklistEvento> items = checklistJpa.findByEventoPrivado_IdOrderByOrdenAsc(idEvento)
                .stream().map(mapper::toDomain).toList();
        if (items.isEmpty()) return items;

        List<UUID> idsUsuarios = items.stream()
                .map(ChecklistEvento::getIdUsuarioCompleto)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, String> nombresPorId = perfilJpa.findAllById(idsUsuarios).stream()
                .collect(Collectors.toMap(PerfilUsuarioEntity::getId, PerfilUsuarioEntity::getNombreCompleto));

        return items.stream()
                .map(item -> item.getIdUsuarioCompleto() == null ? item
                        : item.toBuilder().nombreUsuarioCompleto(nombresPorId.get(item.getIdUsuarioCompleto())).build())
                .toList();
    }

    @Override
    @Transactional
    public ChecklistEvento save(ChecklistEvento checklist) {
        var evento = eventoJpa.findById(checklist.getIdEventoPrivado())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", checklist.getIdEventoPrivado()));
        return enriquecer(mapper.toDomain(checklistJpa.save(mapper.toEntity(checklist, evento))));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        checklistJpa.deleteById(id);
    }

    @Override
    @Transactional
    public void crearTareasBase(Long idEvento, List<String> tareas) {
        EventoPrivadoEntity evento = eventoJpa.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", idEvento));
        List<ChecklistEventoEntity> entidades = new ArrayList<>();
        for (int i = 0; i < tareas.size(); i++) {
            entidades.add(ChecklistEventoEntity.builder()
                    .eventoPrivado(evento)
                    .tarea(tareas.get(i))
                    .orden(i + 1)
                    .build());
        }
        checklistJpa.saveAll(entidades);
    }

    private ChecklistEvento enriquecer(ChecklistEvento d) {
        if (d.getIdUsuarioCompleto() == null) return d;
        String nombre = perfilJpa.findByIdAndDeletedAtIsNull(d.getIdUsuarioCompleto())
                .map(p -> p.getNombreCompleto())
                .orElse(null);
        return d.toBuilder().nombreUsuarioCompleto(nombre).build();
    }
}
