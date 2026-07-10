package com.playzone.pems.infrastructure.persistence.evento.jpa;

import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface EventoPrivadoJpaRepository extends JpaRepository<EventoPrivadoEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM EventoPrivadoEntity e WHERE e.id = :id")
    Optional<EventoPrivadoEntity> findByIdForUpdate(@Param("id") Long id);

    Page<EventoPrivadoEntity> findByClienteId(Long clienteId, Pageable pageable);

    Page<EventoPrivadoEntity> findBySede_IdAndEstado(Long idSede, EstadoEventoPrivado estado, Pageable pageable);

    Page<EventoPrivadoEntity> findBySede_IdAndFechaEventoBetween(
            Long idSede, LocalDate inicio, LocalDate fin, Pageable pageable);

    List<EventoPrivadoEntity> findBySede_IdAndFechaEventoBetween(Long idSede, LocalDate inicio, LocalDate fin);

    List<EventoPrivadoEntity> findBySede_IdAndFechaEvento(Long idSede, LocalDate fecha);

    @Query("""
            SELECT e FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede
              AND e.fechaEvento BETWEEN :inicio AND :fin
              AND e.estado IN ('SOLICITADA', 'CONFIRMADA', 'COMPLETADA')
            ORDER BY e.fechaEvento, e.turno.codigo
            """)
    List<EventoPrivadoEntity> findActivosBySedeAndFechaBetween(
            @Param("idSede") Long      idSede,
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin);

    @Query("""
            SELECT e FROM EventoPrivadoEntity e
            WHERE (CAST(:idSede AS long) IS NULL OR e.sede.id = :idSede)
              AND (:estadoEnum IS NULL OR e.estado = :estadoEnum)
              AND (CAST(:fechaDesde AS localdate) IS NULL OR e.fechaEvento >= :fechaDesde)
              AND (CAST(:fechaHasta AS localdate) IS NULL OR e.fechaEvento <= :fechaHasta)
              AND (:tipoEvento IS NULL OR e.tipoEvento = :tipoEvento)
              AND (:modalidadPago IS NULL OR e.modalidadPago = :modalidadPago)
              AND (:searchPattern IS NULL OR LOWER(e.tipoEvento) LIKE :searchPattern)
            """)
    Page<EventoPrivadoEntity> buscarAdmin(
            @Param("idSede")        Long                idSede,
            @Param("estadoEnum")    EstadoEventoPrivado estadoEnum,
            @Param("fechaDesde")    LocalDate           fechaDesde,
            @Param("fechaHasta")    LocalDate           fechaHasta,
            @Param("tipoEvento")    String              tipoEvento,
            @Param("modalidadPago") String              modalidadPago,
            @Param("searchPattern") String              searchPattern,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) > 0 FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede AND e.fechaEvento = :fecha
              AND e.turno.codigo = (CASE WHEN :idTurno = 1 THEN 'T1' WHEN :idTurno = 2 THEN 'T2' ELSE '' END)
              AND e.estado IN ('SOLICITADA','CONFIRMADA')
            """)
    boolean existsActivoBySedeAndFechaAndTurno(
            @Param("idSede") Long idSede,
            @Param("fecha")  LocalDate fecha,
            @Param("idTurno")Long idTurno);

    @Query("""
            SELECT COUNT(e) > 0 FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede AND e.fechaEvento = :fecha
              AND e.turno.codigo = :codigoTurno AND e.estado IN ('SOLICITADA','CONFIRMADA')
            """)
    boolean existsActivoBySedeAndFechaAndCodigoTurno(
            @Param("idSede")       Long      idSede,
            @Param("fecha")        LocalDate fecha,
            @Param("codigoTurno")  String    codigoTurno);

    @Query("""
            SELECT e FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede AND e.fechaEvento = :fecha
              AND e.estado IN ('SOLICITADA','CONFIRMADA')
            ORDER BY e.turno.codigo
            """)
    List<EventoPrivadoEntity> findActivosBySedeAndFecha(
            @Param("idSede") Long idSede,
            @Param("fecha")  LocalDate fecha);

    @Query("""
            SELECT COUNT(e) > 0 FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede
              AND e.fechaEvento = :fecha
              AND e.estado IN ('SOLICITADA','CONFIRMADA')
            """)
    boolean existsActivoBySedeAndFecha(
            @Param("idSede") Long idSede,
            @Param("fecha")  LocalDate fecha);

    @Query("SELECT COALESCE(SUM(e.montoAdelanto), 0) FROM EventoPrivadoEntity e " +
           "WHERE e.sede.id = :idSede AND YEAR(e.fechaEvento) = :anio " +
           "AND MONTH(e.fechaEvento) = :mes AND e.estado <> 'CANCELADA'")
    BigDecimal sumAdelantosBySedeAndPeriodo(
            @Param("idSede") Long idSede,
            @Param("anio") int anio,
            @Param("mes") int mes);

    @Query("SELECT COALESCE(SUM(e.precioContrato - e.montoAdelanto), 0) FROM EventoPrivadoEntity e " +
           "WHERE e.sede.id = :idSede AND YEAR(e.fechaEvento) = :anio AND MONTH(e.fechaEvento) = :mes " +
           "AND e.estado = 'CONFIRMADA' AND e.precioContrato > e.montoAdelanto")
    BigDecimal sumSaldoPendienteBySedeAndMes(
            @Param("idSede") Long idSede,
            @Param("anio") int anio,
            @Param("mes") int mes);

    @Query("SELECT COUNT(e) FROM EventoPrivadoEntity e WHERE e.sede.id = :idSede AND e.estado = :estado")
    int countBySedeAndEstado(@Param("idSede") Long idSede, @Param("estado") EstadoEventoPrivado estado);

    @Query("""
            SELECT COUNT(e) FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede
              AND e.fechaEvento BETWEEN :inicio AND :fin
              AND e.estado = :estado
            """)
    int countBySedeAndRangoAndEstado(
            @Param("idSede") Long idSede,
            @Param("inicio") LocalDate inicio,
            @Param("fin")    LocalDate fin,
            @Param("estado") EstadoEventoPrivado estado);

    @Query("""
            SELECT COUNT(e) FROM EventoPrivadoEntity e
            WHERE e.sede.id = :idSede
              AND e.estado = 'CONFIRMADA'
              AND e.precioContrato IS NOT NULL
              AND e.montoAdelanto < e.precioContrato
            """)
    int countConfirmadosConSaldo(@Param("idSede") Long idSede);
}
