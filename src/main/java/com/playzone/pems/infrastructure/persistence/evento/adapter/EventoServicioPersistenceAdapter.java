package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoServicio;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioCotizacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoServicioEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoServicioJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        List<EventoServicioEntity> entities = servicios.stream().map(s -> {
            var eventoEntity = eventoJpa.findById(s.getIdEventoPrivado())
                    .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", s.getIdEventoPrivado()));
            var servicioEntity = s.getIdServicioCotizacion() != null
                    ? servicioCotizacionJpa.findById(s.getIdServicioCotizacion()).orElse(null) : null;
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
