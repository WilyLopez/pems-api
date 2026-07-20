package com.playzone.pems.infrastructure.persistence.evento.jpa;

import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoCuotaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoCuotaJpaRepository extends JpaRepository<EventoCuotaEntity, Long> {

    List<EventoCuotaEntity> findByEventoIdOrderByNumeroCuotaAsc(Long eventoId);

    List<EventoCuotaEntity> findByEstadoAndFechaVencimientoBefore(EstadoCuota estado, LocalDate fecha);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM EventoCuotaEntity c WHERE c.id = :id")
    Optional<EventoCuotaEntity> findByIdForUpdate(@Param("id") Long id);
}
