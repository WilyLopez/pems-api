package com.playzone.pems.domain.evento.repository;

import com.playzone.pems.domain.evento.model.EventoCuota;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoCuotaRepository {
    EventoCuota save(EventoCuota cuota);
    List<EventoCuota> saveAll(List<EventoCuota> cuotas);
    Optional<EventoCuota> findById(Long id);
    Optional<EventoCuota> findByIdForUpdate(Long id);
    List<EventoCuota> findByEventoId(Long eventoId);
    List<EventoCuota> findPendientesVencidosAntes(LocalDate fecha);
}
