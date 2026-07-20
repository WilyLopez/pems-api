package com.playzone.pems.domain.evento.repository;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EventoPrivadoRepository {

    Optional<EventoPrivado> findById(Long id);

    Optional<EventoPrivado> findByIdForUpdate(Long id);

    Page<EventoPrivado> findByCliente(Long idCliente, Pageable pageable);

    Page<EventoPrivado> findBySedeAndEstado(
            Long idSede, EstadoEventoPrivado estado, Pageable pageable);

    Page<EventoPrivado> findBySedeAndFechasBetween(
            Long idSede, LocalDate inicio, LocalDate fin, Pageable pageable);

    List<EventoPrivado> findBySedeAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin);

    List<EventoPrivado> findActivosBySedeAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin);

    List<EventoPrivado> findBySedeAndFecha(Long idSede, LocalDate fecha);

    boolean existsActivoBySedeAndFechaAndCodigoTurno(Long idSede, LocalDate fecha, String codigoTurno);

    boolean existsActivoBySedeAndFecha(Long idSede, LocalDate fecha);

    List<EventoPrivado> findActivosBySedeAndFecha(Long idSede, LocalDate fecha);

    Page<EventoPrivado> buscarAdmin(
            Long idSede, EstadoEventoPrivado estadoEnum,
            LocalDate fechaDesde, LocalDate fechaHasta,
            String tipoEvento, String modalidadPago,
            String searchPattern, Pageable pageable);

    EventoPrivado save(EventoPrivado evento);

    BigDecimal sumAdelantosBySedeAndPeriodo(Long idSede, int anio, int mes);

    BigDecimal sumSaldoPendienteBySedeAndMes(Long idSede, int anio, int mes);

    int countBySedeAndEstado(Long idSede, EstadoEventoPrivado estado);

    int countBySedeAndRangoAndEstado(Long idSede, LocalDate inicio, LocalDate fin, EstadoEventoPrivado estado);

    int countConfirmadosConSaldo(Long idSede);
}