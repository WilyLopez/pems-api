package com.playzone.pems.infrastructure.persistence.finanzas.jpa;

import com.playzone.pems.infrastructure.persistence.finanzas.entity.RegistroIngresoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface RegistroIngresoJpaRepository extends JpaRepository<RegistroIngresoEntity, Long> {

    String FILTRO_VIGENTE =
            "r.naturaleza = com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja.NORMAL " +
            "AND NOT EXISTS (SELECT 1 FROM RegistroIngresoEntity r2 WHERE r2.registroAnuladoId = r.id)";

    boolean existsByRegistroAnuladoId(Long registroAnuladoId);

    @Query("SELECT r FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin " +
           "AND r.medioPagoCodigo IS NOT NULL AND r.medioPagoCodigo <> 'EFECTIVO' " +
           "AND r.reservaId IN (" +
           "  SELECT rp.id FROM ReservaPublicaEntity rp " +
           "  WHERE rp.canalReserva = com.playzone.pems.domain.evento.model.enums.CanalReserva.WEB" +
           ") " +
           "ORDER BY r.fecha DESC")
    List<RegistroIngresoEntity> findTesoreriaWeb(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query(value = "SELECT r FROM RegistroIngresoEntity r WHERE r.sede.id = :idSede ORDER BY r.fecha DESC",
           countQuery = "SELECT COUNT(r) FROM RegistroIngresoEntity r WHERE r.sede.id = :idSede")
    Page<RegistroIngresoEntity> findBySede_IdWithTipo(@Param("idSede") Long idSede, Pageable pageable);

    @Query("SELECT r FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin ORDER BY r.fecha DESC")
    List<RegistroIngresoEntity> findBySede_IdAndFechaBetweenWithTipo(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT r FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND YEAR(r.fecha) = :anio AND MONTH(r.fecha) = :mes ORDER BY r.fecha DESC")
    List<RegistroIngresoEntity> findBySede_IdAndPeriodoWithTipo(
            @Param("idSede") Long idSede, @Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(r.monto), 0) FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND YEAR(r.fecha) = :anio AND MONTH(r.fecha) = :mes " +
           "AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndPeriodo(
            @Param("idSede") Long idSede, @Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(r.monto), 0) FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin " +
           "AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndRango(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT COALESCE(SUM(r.monto), 0) FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fechaCobro BETWEEN :inicio AND :fin " +
           "AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndRangoCobro(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT r.tipoCodigo, COALESCE(SUM(r.monto), 0) FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND YEAR(r.fecha) = :anio AND MONTH(r.fecha) = :mes " +
           "AND " + FILTRO_VIGENTE + " " +
           "GROUP BY r.tipoCodigo")
    List<Object[]> sumMontoAgrupadoPorTipo(
            @Param("idSede") Long idSede, @Param("anio") int anio, @Param("mes") int mes);

    @Query("SELECT r.fecha, COALESCE(SUM(r.monto), 0) FROM RegistroIngresoEntity r " +
           "WHERE r.sede.id = :idSede AND r.fecha BETWEEN :inicio AND :fin " +
           "AND " + FILTRO_VIGENTE + " " +
           "GROUP BY r.fecha ORDER BY r.fecha")
    List<Object[]> sumMontoAgrupadoPorDia(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);
}
