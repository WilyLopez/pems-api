package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoExtra;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ExtraPaqueteEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ExtraPaqueteJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoExtraEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoExtraJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class EventoExtraPersistenceAdapter implements EventoExtraRepository {

    private final EventoExtraJpaRepository    extraJpa;
    private final EventoPrivadoJpaRepository  eventoJpa;
    private final ExtraPaqueteJpaRepository   extraPaqueteJpa;

    @Override
    public List<EventoExtra> findByEvento(Long idEventoPrivado) {
        return extraJpa.findByEvento_Id(idEventoPrivado)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public List<EventoExtra> saveAll(List<EventoExtra> extras) {
        if (extras.isEmpty()) return List.of();

        Map<Long, EventoPrivadoEntity> eventosPorId = eventoJpa
                .findAllById(extras.stream().map(EventoExtra::getIdEventoPrivado).distinct().toList())
                .stream().collect(Collectors.toMap(EventoPrivadoEntity::getId, Function.identity()));

        Map<Long, ExtraPaqueteEntity> extrasPorId = extraPaqueteJpa
                .findAllById(extras.stream().map(EventoExtra::getIdExtra)
                        .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(ExtraPaqueteEntity::getId, Function.identity()));

        List<EventoExtraEntity> entities = extras.stream().map(e -> {
            var eventoEntity = eventosPorId.get(e.getIdEventoPrivado());
            if (eventoEntity == null) {
                throw new ResourceNotFoundException("EventoPrivado", e.getIdEventoPrivado());
            }
            var extraEntity = e.getIdExtra() != null ? extrasPorId.get(e.getIdExtra()) : null;
            return EventoExtraEntity.builder()
                    .evento(eventoEntity)
                    .extra(extraEntity)
                    .nombreLibre(e.getNombreLibre())
                    .cantidad(e.getCantidad())
                    .notas(e.getNotas())
                    .build();
        }).toList();
        return extraJpa.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByEvento(Long idEventoPrivado) {
        extraJpa.deleteByEvento_Id(idEventoPrivado);
    }

    private EventoExtra toDomain(EventoExtraEntity e) {
        return EventoExtra.builder()
                .id(e.getId())
                .idEventoPrivado(e.getEvento().getId())
                .idExtra(e.getExtra() != null ? e.getExtra().getId() : null)
                .nombreLibre(e.getNombreLibre())
                .cantidad(e.getCantidad())
                .notas(e.getNotas())
                .build();
    }
}
