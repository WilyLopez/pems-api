package com.playzone.pems.infrastructure.persistence.notificacion.jpa;

import com.playzone.pems.infrastructure.persistence.notificacion.entity.NotificacionEntregaEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.QueryHint;
import java.util.List;

public interface NotificacionEntregaJpaRepository extends JpaRepository<NotificacionEntregaEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT e FROM NotificacionEntregaEntity e WHERE e.canal = 'EMAIL' AND e.estado = 'PENDIENTE' ORDER BY e.fechaCreacion")
    List<NotificacionEntregaEntity> findPendientesEmail(Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT e FROM NotificacionEntregaEntity e WHERE e.canal = 'EMAIL' AND e.estado = 'ERROR' AND e.intentos < :maxIntentos ORDER BY e.fechaCreacion")
    List<NotificacionEntregaEntity> findParaReintentarEmail(@Param("maxIntentos") int maxIntentos, Pageable pageable);

    List<NotificacionEntregaEntity> findByNotificacion_Id(Long notificacionId);
}
