package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.infrastructure.persistence.calendario.jpa.TurnoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.mapper.EventoPrivadoEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EventoPrivadoPersistenceAdapter implements EventoPrivadoRepository {

    private final EventoPrivadoJpaRepository eventoJpa;
    private final SedeJpaRepository          sedeJpa;
    private final TurnoJpaRepository         turnoJpa;
    private final EventoPrivadoEntityMapper  mapper;

    @Override public Optional<EventoPrivado> findById(Long id) {
        return eventoJpa.findById(id).map(mapper::toDomain);
    }

    @Override public Optional<EventoPrivado> findByIdForUpdate(Long id) {
        return eventoJpa.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override public Page<EventoPrivado> findByCliente(Long idCliente, Pageable pageable) {
        return eventoJpa.findByClienteId(idCliente, pageable).map(mapper::toDomain);
    }

    @Override public Page<EventoPrivado> findBySedeAndEstado(Long idSede, EstadoEventoPrivado estado, Pageable pageable) {
        return eventoJpa.findBySede_IdAndEstado(idSede, estado, pageable).map(mapper::toDomain);
    }

    @Override public Page<EventoPrivado> findBySedeAndFechasBetween(Long idSede, LocalDate inicio, LocalDate fin, Pageable pageable) {
        return eventoJpa.findBySede_IdAndFechaEventoBetween(idSede, inicio, fin, pageable).map(mapper::toDomain);
    }

    @Override public List<EventoPrivado> findBySedeAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin) {
        return eventoJpa.findBySede_IdAndFechaEventoBetween(idSede, inicio, fin).stream().map(mapper::toDomain).toList();
    }

    @Override public List<EventoPrivado> findActivosBySedeAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin) {
        return eventoJpa.findActivosBySedeAndFechaBetween(idSede, inicio, fin).stream().map(mapper::toDomain).toList();
    }

    @Override public List<EventoPrivado> findBySedeAndFecha(Long idSede, LocalDate fecha) {
        return eventoJpa.findBySede_IdAndFechaEvento(idSede, fecha).stream().map(mapper::toDomain).toList();
    }

    @Override public boolean existsActivoBySedeAndFechaAndTurno(Long idSede, LocalDate fecha, Long idTurno) {
        return eventoJpa.existsActivoBySedeAndFechaAndTurno(idSede, fecha, idTurno);
    }

    @Override public boolean existsActivoBySedeAndFechaAndCodigoTurno(Long idSede, LocalDate fecha, String codigoTurno) {
        return eventoJpa.existsActivoBySedeAndFechaAndCodigoTurno(idSede, fecha, codigoTurno);
    }

    @Override public boolean existsActivoBySedeAndFecha(Long idSede, LocalDate fecha) {
        return eventoJpa.existsActivoBySedeAndFecha(idSede, fecha);
    }

    @Override public List<EventoPrivado> findActivosBySedeAndFecha(Long idSede, LocalDate fecha) {
        return eventoJpa.findActivosBySedeAndFecha(idSede, fecha).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<EventoPrivado> buscarAdmin(
            Long idSede, EstadoEventoPrivado estadoEnum,
            LocalDate fechaDesde, LocalDate fechaHasta,
            String tipoEvento, String modalidadPago,
            String searchPattern, Pageable pageable) {
        return eventoJpa.buscarAdmin(idSede, estadoEnum, fechaDesde, fechaHasta,
                        tipoEvento, modalidadPago, searchPattern, pageable)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public EventoPrivado save(EventoPrivado evento) {
        var sede = sedeJpa.findById(evento.getIdSede())
                .orElseThrow(() -> new ResourceNotFoundException("Sede", evento.getIdSede()));
        Long idTurno = evento.getIdTurno();
        String codigoTurno = idTurno == 1L ? "T1" : idTurno == 2L ? "T2" : String.valueOf(idTurno);
        var turno = turnoJpa.findById(codigoTurno)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", idTurno));

        return mapper.toDomain(eventoJpa.save(mapper.toEntity(evento, sede, turno)));
    }

    @Override public BigDecimal sumAdelantosBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return eventoJpa.sumAdelantosBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override public BigDecimal sumSaldoPendienteBySedeAndMes(Long idSede, int anio, int mes) {
        return eventoJpa.sumSaldoPendienteBySedeAndMes(idSede, anio, mes);
    }

    @Override public int countBySedeAndEstado(Long idSede, EstadoEventoPrivado estado) {
        return eventoJpa.countBySedeAndEstado(idSede, estado);
    }

    @Override public int countBySedeAndRangoAndEstado(Long idSede, LocalDate inicio, LocalDate fin, EstadoEventoPrivado estado) {
        return eventoJpa.countBySedeAndRangoAndEstado(idSede, inicio, fin, estado);
    }

    @Override public int countConfirmadosConSaldo(Long idSede) {
        return eventoJpa.countConfirmadosConSaldo(idSede);
    }
}
