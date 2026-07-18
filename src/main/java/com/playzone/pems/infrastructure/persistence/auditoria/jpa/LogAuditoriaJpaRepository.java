package com.playzone.pems.infrastructure.persistence.auditoria.jpa;

import com.playzone.pems.infrastructure.persistence.auditoria.entity.LogAuditoriaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface LogAuditoriaJpaRepository extends JpaRepository<LogAuditoriaEntity, Long> {

    Page<LogAuditoriaEntity> findByUsuarioIdOrderByFechaLogDesc(UUID usuarioId, Pageable pageable);

    Page<LogAuditoriaEntity> findByModuloAndEntidadAfectadaOrderByFechaLogDesc(
            String modulo, String entidad, Pageable pageable);

    Page<LogAuditoriaEntity> findByEntidadAfectadaAndIdEntidadOrderByFechaLogDesc(
            String entidadAfectada, Long idEntidad, Pageable pageable);

    Page<LogAuditoriaEntity> findByFechaLogBetweenOrderByFechaLogDesc(
            OffsetDateTime desde, OffsetDateTime hasta, Pageable pageable);

    @Query(value = """
            SELECT l FROM LogAuditoriaEntity l
            WHERE l.fechaLog BETWEEN :desde AND :hasta
              AND (:idUsuario IS NULL OR l.usuarioId  = :idUsuario)
              AND (:modulo    IS NULL OR LOWER(l.modulo)          LIKE LOWER(CONCAT('%', :modulo,    '%')))
              AND (:accion    IS NULL OR l.accion                 = :accion)
              AND (:entidad   IS NULL OR LOWER(l.entidadAfectada) LIKE LOWER(CONCAT('%', :entidad,   '%')))
              AND (:nivel     IS NULL OR l.nivel                  = :nivel)
              AND (:resultado IS NULL OR l.resultado              = :resultado)
            ORDER BY l.fechaLog DESC
            """,
           countQuery = """
            SELECT COUNT(l) FROM LogAuditoriaEntity l
            WHERE l.fechaLog BETWEEN :desde AND :hasta
              AND (:idUsuario IS NULL OR l.usuarioId  = :idUsuario)
              AND (:modulo    IS NULL OR LOWER(l.modulo)          LIKE LOWER(CONCAT('%', :modulo,    '%')))
              AND (:accion    IS NULL OR l.accion                 = :accion)
              AND (:entidad   IS NULL OR LOWER(l.entidadAfectada) LIKE LOWER(CONCAT('%', :entidad,   '%')))
              AND (:nivel     IS NULL OR l.nivel                  = :nivel)
              AND (:resultado IS NULL OR l.resultado              = :resultado)
            """)
    Page<LogAuditoriaEntity> findByFiltros(
            @Param("desde")     OffsetDateTime desde,
            @Param("hasta")     OffsetDateTime hasta,
            @Param("idUsuario") UUID           idUsuario,
            @Param("modulo")    String         modulo,
            @Param("accion")    String         accion,
            @Param("entidad")   String         entidad,
            @Param("nivel")     String         nivel,
            @Param("resultado") String         resultado,
            Pageable pageable);
}
