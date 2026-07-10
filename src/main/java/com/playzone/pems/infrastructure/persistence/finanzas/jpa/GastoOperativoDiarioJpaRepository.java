package com.playzone.pems.infrastructure.persistence.finanzas.jpa;

import com.playzone.pems.infrastructure.persistence.finanzas.entity.GastoOperativoDiarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface GastoOperativoDiarioJpaRepository extends JpaRepository<GastoOperativoDiarioEntity, Long> {

    String FILTRO_VIGENTE =
            "g.naturaleza = com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja.NORMAL " +
            "AND NOT EXISTS (SELECT 1 FROM GastoOperativoDiarioEntity g2 WHERE g2.gastoAnuladoId = g.id)";

    boolean existsByGastoAnuladoId(Long gastoAnuladoId);

    List<GastoOperativoDiarioEntity> findBySede_IdAndFecha(Long idSede, LocalDate fecha);

    List<GastoOperativoDiarioEntity> findBySede_IdAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoOperativoDiarioEntity g " +
           "WHERE g.sede.id = :idSede AND g.fecha = :fecha AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndFecha(@Param("idSede") Long idSede, @Param("fecha") LocalDate fecha);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoOperativoDiarioEntity g " +
           "WHERE g.sede.id = :idSede AND YEAR(g.fecha) = :anio AND MONTH(g.fecha) = :mes AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndPeriodo(
            @Param("idSede") Long idSede,
            @Param("anio") int anio,
            @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(g.monto), 0) FROM GastoOperativoDiarioEntity g " +
           "WHERE g.sede.id = :idSede AND g.fecha BETWEEN :inicio AND :fin AND " + FILTRO_VIGENTE)
    BigDecimal sumMontoBySedeAndRango(
            @Param("idSede") Long idSede, @Param("inicio") LocalDate inicio, @Param("fin") LocalDate fin);

    @Query("SELECT new com.playzone.pems.domain.finanzas.query.GastosPorDia(" +
           "g.fecha, COALESCE(SUM(g.monto), 0)) " +
           "FROM GastoOperativoDiarioEntity g " +
           "WHERE g.sede.id = :idSede " +
           "AND g.fecha BETWEEN :inicio AND :fin " +
           "AND " + FILTRO_VIGENTE + " " +
           "GROUP BY g.fecha " +
           "ORDER BY g.fecha")
    List<com.playzone.pems.domain.finanzas.query.GastosPorDia> sumMontoAgrupadoPorDia(
            @Param("idSede") Long idSede,
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);
}
