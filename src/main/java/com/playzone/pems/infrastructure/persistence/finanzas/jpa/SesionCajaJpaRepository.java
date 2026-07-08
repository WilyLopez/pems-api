package com.playzone.pems.infrastructure.persistence.finanzas.jpa;

import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.SesionCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionCajaJpaRepository extends JpaRepository<SesionCajaEntity, Long> {

    Optional<SesionCajaEntity> findByUsuarioIdAndEstado(UUID usuarioId, EstadoCaja estado);

    Optional<SesionCajaEntity> findByUsuarioIdAndSede_IdAndEstado(UUID usuarioId, Long idSede, EstadoCaja estado);

    boolean existsBySede_IdAndEstado(Long idSede, EstadoCaja estado);

    Optional<SesionCajaEntity> findFirstByUsuarioIdAndSede_IdAndFechaAperturaBetweenOrderByFechaAperturaDesc(
            UUID usuarioId, Long idSede, OffsetDateTime desde, OffsetDateTime hasta);

    List<SesionCajaEntity> findBySede_IdAndFechaAperturaBetweenOrderByFechaAperturaDesc(
            Long idSede, OffsetDateTime desde, OffsetDateTime hasta);

    @Modifying
    @Query("UPDATE SesionCajaEntity s SET s.totalIngresos = s.totalIngresos + :delta WHERE s.id = :id")
    int incrementarIngresos(@Param("id") Long id, @Param("delta") BigDecimal delta);

    @Modifying
    @Query("UPDATE SesionCajaEntity s SET s.totalEgresos = s.totalEgresos + :delta WHERE s.id = :id")
    int incrementarEgresos(@Param("id") Long id, @Param("delta") BigDecimal delta);
}
