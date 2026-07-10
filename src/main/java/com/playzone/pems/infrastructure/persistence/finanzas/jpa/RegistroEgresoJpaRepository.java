package com.playzone.pems.infrastructure.persistence.finanzas.jpa;

import com.playzone.pems.infrastructure.persistence.finanzas.entity.RegistroEgresoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegistroEgresoJpaRepository extends JpaRepository<RegistroEgresoEntity, Long> {

    String FILTRO_VIGENTE =
            "r.naturaleza = com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja.NORMAL " +
            "AND r.estadoAprobacion = com.playzone.pems.domain.finanzas.model.enums.EstadoAprobacionEgreso.APROBADO " +
            "AND NOT EXISTS (SELECT 1 FROM RegistroEgresoEntity r2 WHERE r2.registroAnuladoId = r.id)";

    boolean existsByRegistroAnuladoId(Long registroAnuladoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistroEgresoEntity r WHERE r.id = :id")
    Optional<RegistroEgresoEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT r FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.estadoAprobacion = " +
           "com.playzone.pems.domain.finanzas.model.enums.EstadoAprobacionEgreso.PENDIENTE_APROBACION " +
           "ORDER BY r.createdAt ASC")
    List<RegistroEgresoEntity> findPendientesAprobacion(@Param("idSede") Long idSede);

    @Query(value = "SELECT r FROM RegistroEgresoEntity r WHERE r.sede.id = :idSede",
           countQuery = "SELECT COUNT(r) FROM RegistroEgresoEntity r WHERE r.sede.id = :idSede")
    Page<RegistroEgresoEntity> findBySede_IdWithTipo(@Param("idSede") Long idSede, Pageable pageable);

    @Query("SELECT r FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.periodoAnio = :anio AND r.periodoMes = :mes")
    List<RegistroEgresoEntity> findBySede_IdAndPeriodoWithTipo(
            @Param("idSede") Long idSede, @Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT r FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin")
    List<RegistroEgresoEntity> findBySede_IdAndFechaBetweenWithTipo(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(r.monto), 0) FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.periodoAnio = :anio AND r.periodoMes = :mes " +
           "AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndPeriodo(
            @Param("idSede") Long idSede,
            @Param("anio") int anio,
            @Param("mes") int mes);

    @Query("SELECT r.tipoCodigo, COALESCE(SUM(r.monto), 0) FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.periodoAnio = :anio AND r.periodoMes = :mes " +
           "AND " + FILTRO_VIGENTE + " " +
           "GROUP BY r.tipoCodigo")
    List<Object[]> sumMontoAgrupadoPorTipo(
            @Param("idSede") Long idSede, @Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(r.monto), 0) FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin " +
           "AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndRango(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT t.categoria, COALESCE(SUM(r.monto), 0) FROM RegistroEgresoEntity r " +
           "JOIN TipoEgresoEntity t ON t.codigo = r.tipoCodigo " +
           "WHERE r.sede.id = :idSede AND r.periodoAnio = :anio AND r.periodoMes = :mes " +
           "AND " + FILTRO_VIGENTE + " " +
           "GROUP BY t.categoria")
    List<Object[]> sumMontoAgrupadoPorCategoria(
            @Param("idSede") Long idSede, @Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT r.fecha, COALESCE(SUM(r.monto), 0) FROM RegistroEgresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin " +
           "AND " + FILTRO_VIGENTE + " " +
           "GROUP BY r.fecha ORDER BY r.fecha")
    List<Object[]> sumMontoAgrupadoPorDia(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}
