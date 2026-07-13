package com.playzone.pems.infrastructure.persistence.evento.adapter;

import com.playzone.pems.application.evento.dto.query.MetricasReservaQuery;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.query.IngresosPorDia;
import com.playzone.pems.domain.evento.query.ReservasPorDia;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.infrastructure.persistence.calendario.jpa.ConfiguracionCalendarioJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.entity.ReservaPublicaEntity;
import com.playzone.pems.infrastructure.persistence.evento.jpa.ReservaPublicaJpaRepository;
import com.playzone.pems.infrastructure.persistence.evento.mapper.ReservaPublicaEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReservaPublicaPersistenceAdapter implements ReservaPublicaRepository {

    private final ReservaPublicaJpaRepository       reservaJpa;
    private final SedeJpaRepository                 sedeJpa;
    private final ReservaPublicaEntityMapper        mapper;
    private final ConfiguracionCalendarioJpaRepository configuracionCalendarioJpa;

    @Override public Optional<ReservaPublica> findById(Long id) {
        return reservaJpa.findById(id).map(mapper::toDomain);
    }

    @Override public Optional<ReservaPublica> findByIdForUpdate(Long id) {
        return reservaJpa.findByIdForUpdate(id).map(mapper::toDomain);
    }

    @Override public Optional<ReservaPublica> findByNumeroTicket(String ticket) {
        return reservaJpa.findByNumeroTicket(ticket).map(mapper::toDomain);
    }

    @Override public Page<ReservaPublica> findByCliente(Long idCliente, Pageable pageable) {
        return reservaJpa.findByClienteId(idCliente, pageable).map(mapper::toDomain);
    }

    @Override public Page<ReservaPublica> findBySedeAndFecha(Long idSede, LocalDate fecha, Pageable pageable) {
        return reservaJpa.findBySede_IdAndFechaEvento(idSede, fecha, pageable).map(mapper::toDomain);
    }

    @Override public List<ReservaPublica> findBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.findBySede_IdAndFechaEvento(idSede, fecha).stream().map(mapper::toDomain).toList();
    }

    @Override public List<ReservaPublica> findBySedeAndFechaBetween(Long idSede, LocalDate inicio, LocalDate fin) {
        return reservaJpa.findBySede_IdAndFechaEventoBetween(idSede, inicio, fin).stream().map(mapper::toDomain).toList();
    }

    @Override public Page<ReservaPublica> findBySedeAndEstado(Long idSede, EstadoReservaPublica estado, Pageable pageable) {
        return reservaJpa.findBySede_IdAndEstado(idSede, estado, pageable).map(mapper::toDomain);
    }

    @Override public List<ReservaPublica> findConfirmadasBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.findBySede_IdAndFechaEventoAndEstado(
                idSede, fecha, EstadoReservaPublica.CONFIRMADA).stream().map(mapper::toDomain).toList();
    }

    @Override public List<ReservaPublica> findPendientesBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.findBySede_IdAndFechaEventoAndEstado(
                idSede, fecha, EstadoReservaPublica.PENDIENTE).stream().map(mapper::toDomain).toList();
    }

    @Override public int countConfirmadasBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.countConfirmadasBySedeAndFecha(idSede, fecha);
    }

    @Override public int countActivasBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.countActivasBySedeAndFecha(idSede, fecha);
    }

    @Override public boolean existsActivaBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.existsActivaBySedeAndFecha(idSede, fecha);
    }

    @Override
    public Page<ReservaPublica> buscarAdmin(
            Long idSede, EstadoReservaPublica estadoEnum, LocalDate fecha,
            Boolean ingresado, Boolean esReprogramacion, String medioPago,
            String searchPattern, Pageable pageable) {
        return reservaJpa.buscarAdmin(
                idSede, estadoEnum, fecha, ingresado, esReprogramacion, medioPago, searchPattern, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public MetricasReservaQuery calcularMetricas(Long idSede, LocalDate fecha) {
        LocalDate dia = fecha != null ? fecha : LocalDate.now(ZoneId.of("America/Lima"));
        int aforoMaximo = configuracionCalendarioJpa.findBySede_Id(idSede)
                .map(c -> c.getAforoMaximo())
                .orElse(60);
        int total      = reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.PENDIENTE)
                       + reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.CONFIRMADA)
                       + reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.CANCELADA)
                       + reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.REPROGRAMADA)
                       + reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.COMPLETADA);
        int pendientes  = reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.PENDIENTE);
        int confirmadas = reservaJpa.countConfirmadasBySedeAndFecha(idSede, dia);
        int canceladas  = reservaJpa.countBySedeAndFechaAndEstado(idSede, dia, EstadoReservaPublica.CANCELADA);
        int ingresados  = reservaJpa.countIngresadosBySedeAndFecha(idSede, dia);

        return MetricasReservaQuery.builder()
                .fecha(dia)
                .totalReservas(total)
                .pendientes(pendientes)
                .confirmadas(confirmadas)
                .canceladas(canceladas)
                .ingresados(ingresados)
                .aforoMaximo(aforoMaximo)
                .aforoOcupado(confirmadas)
                .aforoRestante(Math.max(0, aforoMaximo - confirmadas))
                .ingresosDia(reservaJpa.sumIngresosBySedeAndFecha(idSede, dia))
                .build();
    }

    @Override
    @Transactional
    public ReservaPublica save(ReservaPublica reserva) {
        var sede = sedeJpa.findById(reserva.getIdSede())
                .orElseThrow(() -> new ResourceNotFoundException("Sede", reserva.getIdSede()));
        ReservaPublicaEntity original = reserva.getIdReservaOriginal() != null
                ? reservaJpa.findById(reserva.getIdReservaOriginal()).orElse(null) : null;
        return mapper.toDomain(reservaJpa.save(mapper.toEntity(reserva, sede, original)));
    }

    @Override public boolean existsByNumeroTicket(String ticket) {
        return reservaJpa.existsByNumeroTicket(ticket);
    }

    @Override public BigDecimal sumIngresosBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return reservaJpa.sumIngresosBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override public BigDecimal sumIngresosBySedeAndFecha(Long idSede, LocalDate fecha) {
        return reservaJpa.sumIngresosBySedeAndFecha(idSede, fecha);
    }

    @Override public BigDecimal sumIngresosBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return reservaJpa.sumIngresosBySedeAndRango(idSede, inicio, fin);
    }

    @Override public long countConfirmadasBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return reservaJpa.countConfirmadasBySedeAndRango(idSede, inicio, fin);
    }

    @Override public long countConfirmadasBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return reservaJpa.countConfirmadasBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override public long countCanceladasBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return reservaJpa.countCanceladasBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override public long countCompletadasBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return reservaJpa.countCompletadasBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override public BigDecimal avgTicketBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return reservaJpa.avgTicketBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override public int countBySedeAndFechaAndEstado(Long idSede, LocalDate fecha, EstadoReservaPublica estado) {
        return reservaJpa.countBySedeAndFechaAndEstado(idSede, fecha, estado);
    }

    @Override public List<ReservasPorDia> countAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin) {
        return reservaJpa.countAgrupadoPorDia(idSede, inicio, fin);
    }

    @Override public List<IngresosPorDia> sumIngresosAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin) {
        return reservaJpa.sumIngresosAgrupadoPorDia(idSede, inicio, fin);
    }

    @Override
    public List<ReservaPublica> findByVentaId(Long ventaId) {
        return reservaJpa.findByVentaId(ventaId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public int countPendientesSinComprobantePorClienteYFecha(Long idCliente, LocalDate fecha) {
        return reservaJpa.countPendientesSinComprobantePorClienteYFecha(idCliente, fecha);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        reservaJpa.deleteById(id);
    }
}
