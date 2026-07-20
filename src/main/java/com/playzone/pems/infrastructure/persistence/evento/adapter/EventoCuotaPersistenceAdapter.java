package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoCuotaEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoCuotaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventoCuotaPersistenceAdapter implements EventoCuotaRepository {

    private final EventoCuotaJpaRepository jpa;

    @Override
    public EventoCuota save(EventoCuota cuota) {
        return toDomain(jpa.save(toEntity(cuota)));
    }

    @Override
    public List<EventoCuota> saveAll(List<EventoCuota> cuotas) {
        return jpa.saveAll(cuotas.stream().map(this::toEntity).toList())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<EventoCuota> findById(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<EventoCuota> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(this::toDomain);
    }

    @Override
    public List<EventoCuota> findByEventoId(Long eventoId) {
        return jpa.findByEventoIdOrderByNumeroCuotaAsc(eventoId)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<EventoCuota> findPendientesVencidosAntes(LocalDate fecha) {
        return jpa.findByEstadoAndFechaVencimientoBefore(EstadoCuota.PENDIENTE, fecha)
                .stream().map(this::toDomain).toList();
    }

    private EventoCuota toDomain(EventoCuotaEntity e) {
        return EventoCuota.builder()
                .id(e.getId())
                .eventoId(e.getEventoId())
                .numeroCuota(e.getNumeroCuota())
                .monto(e.getMonto())
                .fechaVencimiento(e.getFechaVencimiento())
                .estado(e.getEstado())
                .ventaId(e.getVentaId())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private EventoCuotaEntity toEntity(EventoCuota d) {
        return EventoCuotaEntity.builder()
                .id(d.getId())
                .eventoId(d.getEventoId())
                .numeroCuota(d.getNumeroCuota())
                .monto(d.getMonto())
                .fechaVencimiento(d.getFechaVencimiento())
                .estado(d.getEstado())
                .ventaId(d.getVentaId())
                .build();
    }
}
