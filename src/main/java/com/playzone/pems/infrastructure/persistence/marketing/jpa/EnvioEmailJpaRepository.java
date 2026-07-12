package com.playzone.pems.infrastructure.persistence.marketing.jpa;

import com.playzone.pems.infrastructure.persistence.marketing.entity.EnvioEmailEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnvioEmailJpaRepository extends JpaRepository<EnvioEmailEntity, Long> {

    Page<EnvioEmailEntity> findByCampanaId(Long campanaId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EnvioEmailEntity e WHERE e.campanaId = :idCampana AND e.estado = 'PENDIENTE' ORDER BY e.id")
    List<EnvioEmailEntity> findPendientesByCampana(
            @Param("idCampana") Long idCampana,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EnvioEmailEntity e WHERE e.estado = 'ERROR' AND e.intentos < :maxIntentos ORDER BY e.createdAt")
    List<EnvioEmailEntity> findParaReintentar(@Param("maxIntentos") int maxIntentos, Pageable pageable);

    long countByCampanaIdAndEstado(Long campanaId, String estado);
}
