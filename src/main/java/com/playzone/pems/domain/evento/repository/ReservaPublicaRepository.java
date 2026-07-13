package com.playzone.pems.domain.evento.repository;

import com.playzone.pems.application.evento.dto.query.MetricasReservaQuery;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.query.IngresosPorDia;
import com.playzone.pems.domain.evento.query.ReservasPorDia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservaPublicaRepository {

    Optional<ReservaPublica> findById(Long id);

    Optional<ReservaPublica> findByIdForUpdate(Long id);

    Optional<ReservaPublica> findByNumeroTicket(String numeroTicket);

    Page<ReservaPublica> findByCliente(Long idCliente, Pageable pageable);

    Page<ReservaPublica> findBySedeAndFecha(Long idSede, LocalDate fecha, Pageable pageable);

    List<ReservaPublica> findBySedeAndFecha(Long idSede, LocalDate fecha);

    List<ReservaPublica> findBySedeAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin);

    Page<ReservaPublica> findBySedeAndEstado(
            Long idSede, EstadoReservaPublica estado, Pageable pageable);

    List<ReservaPublica> findConfirmadasBySedeAndFecha(Long idSede, LocalDate fecha);

    List<ReservaPublica> findPendientesBySedeAndFecha(Long idSede, LocalDate fecha);

    int countConfirmadasBySedeAndFecha(Long idSede, LocalDate fecha);

    int countActivasBySedeAndFecha(Long idSede, LocalDate fecha);

    boolean existsActivaBySedeAndFecha(Long idSede, LocalDate fecha);

    ReservaPublica save(ReservaPublica reserva);

    boolean existsByNumeroTicket(String numeroTicket);

    Page<ReservaPublica> buscarAdmin(
            Long idSede, EstadoReservaPublica estadoEnum, LocalDate fecha,
            Boolean ingresado, Boolean esReprogramacion, String medioPago,
            String searchPattern, Pageable pageable);

    MetricasReservaQuery calcularMetricas(Long idSede, LocalDate fecha);

    BigDecimal sumIngresosBySedeAndPeriodo(Long idSede, int anio, int mes);

    BigDecimal sumIngresosBySedeAndFecha(Long idSede, LocalDate fecha);

    BigDecimal sumIngresosBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);

    long countConfirmadasBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);

    long countConfirmadasBySedeAndPeriodo(Long idSede, int anio, int mes);

    long countCanceladasBySedeAndPeriodo(Long idSede, int anio, int mes);

    long countCompletadasBySedeAndPeriodo(Long idSede, int anio, int mes);

    BigDecimal avgTicketBySedeAndPeriodo(Long idSede, int anio, int mes);

    int countBySedeAndFechaAndEstado(Long idSede, LocalDate fecha, EstadoReservaPublica estado);

    List<ReservasPorDia> countAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin);

    List<IngresosPorDia> sumIngresosAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin);

    List<ReservaPublica> findByVentaId(Long ventaId);

    int countPendientesSinComprobantePorClienteYFecha(Long idCliente, LocalDate fecha);

    void deleteById(Long id);
}