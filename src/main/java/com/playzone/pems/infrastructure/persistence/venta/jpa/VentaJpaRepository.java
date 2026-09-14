package com.playzone.pems.infrastructure.persistence.venta.jpa;

import com.playzone.pems.infrastructure.persistence.venta.entity.VentaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentaJpaRepository extends JpaRepository<VentaEntity, Long> {

    Page<VentaEntity> findBySede_IdAndCreatedAtBetween(
            Long idSede, OffsetDateTime desde, OffsetDateTime hasta, Pageable pageable);

    Optional<VentaEntity> findByCreatedByAndIdempotencyKey(UUID createdBy, String idempotencyKey);

    Page<VentaEntity> findBySede_IdAndCreatedAtBetweenAndCreatedBy(
            Long idSede, OffsetDateTime desde, OffsetDateTime hasta, UUID createdBy, Pageable pageable);

    List<VentaEntity> findByEventoId(Long eventoId);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM VentaEntity v " +
            "LEFT JOIN ClientePerfilEntity cp ON v.clienteId = cp.id " +
            "WHERE v.sede.id = :idSede " +
            "AND v.createdAt BETWEEN :desde AND :hasta " +
            "AND (:search IS NULL OR :search = '' " +
            "OR CAST(v.id AS string) LIKE %:search% " +
            "OR LOWER(v.nombreAcompanante) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(v.dniAcompanante) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.nombreCompleto) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.correo) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<VentaEntity> findBySedeAndCreatedAtBetweenAndSearch(
            @org.springframework.data.repository.query.Param("idSede") Long idSede,
            @org.springframework.data.repository.query.Param("desde") OffsetDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") OffsetDateTime hasta,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT v FROM VentaEntity v " +
            "LEFT JOIN ClientePerfilEntity cp ON v.clienteId = cp.id " +
            "WHERE v.sede.id = :idSede " +
            "AND v.createdAt BETWEEN :desde AND :hasta " +
            "AND v.createdBy = :createdBy " +
            "AND (:search IS NULL OR :search = '' " +
            "OR CAST(v.id AS string) LIKE %:search% " +
            "OR LOWER(v.nombreAcompanante) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(v.dniAcompanante) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.nombreCompleto) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(cp.correo) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<VentaEntity> findBySedeAndCreatedAtBetweenAndCreatedByAndSearch(
            @org.springframework.data.repository.query.Param("idSede") Long idSede,
            @org.springframework.data.repository.query.Param("desde") OffsetDateTime desde,
            @org.springframework.data.repository.query.Param("hasta") OffsetDateTime hasta,
            @org.springframework.data.repository.query.Param("createdBy") UUID createdBy,
            @org.springframework.data.repository.query.Param("search") String search,
            Pageable pageable);
}
