package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoServicio;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioCotizacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoServicioEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoServicioJpaRepository;
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
public class EventoServicioPersistenceAdapter implements EventoServicioRepository {

    private final EventoServicioJpaRepository   servicioJpa;
    private final EventoPrivadoJpaRepository    eventoJpa;
    private final ServicioCotizacionJpaRepository servicioCotizacionJpa;

    @Override
    public List<EventoServicio> findByEvento(Long idEventoPrivado) {
        return servicioJpa.findByEvento_Id(idEventoPrivado)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public List<EventoServicio> saveAll(List<EventoServicio> servicios) {
        if (servicios.isEmpty()) return List.of();

        Map<Long, EventoPrivadoEntity> eventosPorId = eventoJpa
                .findAllById(servicios.stream().map(EventoServicio::getIdEventoPrivado).distinct().toList())
                .stream().collect(Collectors.toMap(EventoPrivadoEntity::getId, Function.identity()));

        Map<Long, ServicioCotizacionEntity> serviciosPorId = servicioCotizacionJpa
                .findAllById(servicios.stream().map(EventoServicio::getIdServicioCotizacion)
                        .filter(Objects::nonNull).distinct().toList())
                .stream().collect(Collectors.toMap(ServicioCotizacionEntity::getId, Function.identity()));

        List<EventoServicioEntity> entities = servicios.stream().map(s -> {
            var eventoEntity = eventosPorId.get(s.getIdEventoPrivado());
            if (eventoEntity == null) {
                throw new ResourceNotFoundException("EventoPrivado", s.getIdEventoPrivado());
            }
            var servicioEntity = s.getIdServicioCotizacion() != null
                    ? serviciosPorId.get(s.getIdServicioCotizacion()) : null;
            return EventoServicioEntity.builder()
                    .evento(eventoEntity)
                    .servicioCotizacion(servicioEntity)
                    .servicioVarianteId(s.getIdServicioVariante())
                    .nombreLibre(s.getNombreLibre())
                    .descripcion(s.getDescripcion())
                    .precioAcordado(s.getPrecioAcordado())
                    .incluido(s.isIncluido())
                    .build();
        }).toList();
        return servicioJpa.saveAll(entities).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public void deleteByEvento(Long idEventoPrivado) {
        servicioJpa.deleteByEvento_Id(idEventoPrivado);
    }

    private EventoServicio toDomain(EventoServicioEntity e) {
        return EventoServicio.builder()
                .id(e.getId())
                .idEventoPrivado(e.getEvento().getId())
                .idServicioCotizacion(e.getServicioCotizacion() != null ? e.getServicioCotizacion().getId() : null)
                .idServicioVariante(e.getServicioVarianteId())
                .nombreLibre(e.getNombreLibre())
                .descripcion(e.getDescripcion())
                .precioAcordado(e.getPrecioAcordado())
                .incluido(e.isIncluido())
                .build();
    }
}
