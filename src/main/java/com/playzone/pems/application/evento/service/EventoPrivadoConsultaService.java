package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.dto.query.KpisEventosQuery;
import com.playzone.pems.application.evento.port.in.BuscarEventosAdminUseCase;
import com.playzone.pems.application.evento.port.in.ConsultarEventosPrivadosUseCase;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EventoPrivadoConsultaService implements ConsultarEventosPrivadosUseCase, BuscarEventosAdminUseCase {

    private final EventoPrivadoRepository  eventoRepository;
    private final SedeScopeValidator       sedeScope;
    private final EventoAccesoValidator    accesoValidator;
    private final EventoPrivadoQueryMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> buscar(
            Long idSede, String estado,
            LocalDate fechaDesde, LocalDate fechaHasta,
            String tipoEvento, String modalidadPago,
            String search, Pageable pageable) {

        sedeScope.validarAccesoFiltro(idSede);

        EstadoEventoPrivado estadoEnum = null;
        if (estado != null && !estado.isBlank()) {
            try { estadoEnum = EstadoEventoPrivado.valueOf(estado); }
            catch (IllegalArgumentException ignored) {}
        }
        String searchPattern = (search != null && !search.isBlank())
                ? "%" + search.toLowerCase() + "%" : null;
        String tipoFiltro = (tipoEvento != null && !tipoEvento.isBlank()) ? tipoEvento : null;
        String modalidadFiltro = (modalidadPago != null && !modalidadPago.isBlank()) ? modalidadPago : null;

        return eventoRepository.buscarAdmin(
                        idSede, estadoEnum, fechaDesde, fechaHasta,
                        tipoFiltro, modalidadFiltro, searchPattern, pageable)
                .map(e -> mapper.toQuery(e, mapper.obtenerCliente(e.getIdCliente()), mapper.obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public KpisEventosQuery kpis(Long idSede) {
        sedeScope.validarAccesoFiltro(idSede);
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());
        return KpisEventosQuery.builder()
                .solicitadas(eventoRepository.countBySedeAndEstado(idSede, EstadoEventoPrivado.SOLICITADA))
                .confirmadas(eventoRepository.countBySedeAndEstado(idSede, EstadoEventoPrivado.CONFIRMADA))
                .completadasEsteMes(eventoRepository.countBySedeAndRangoAndEstado(
                        idSede, inicioMes, finMes, EstadoEventoPrivado.COMPLETADA))
                .conSaldoPendiente(eventoRepository.countConfirmadosConSaldo(idSede))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> consultarPorCliente(Long idCliente, Pageable pageable) {
        return eventoRepository.findByCliente(idCliente, pageable)
                .map(e -> mapper.toQuery(e, mapper.obtenerCliente(e.getIdCliente()), mapper.obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> consultarPorSedeYEstado(Long idSede, String estado, Pageable pageable) {
        sedeScope.validarAcceso(idSede);
        return eventoRepository.findBySedeAndEstado(idSede, EstadoEventoPrivado.valueOf(estado), pageable)
                .map(e -> mapper.toQuery(e, mapper.obtenerCliente(e.getIdCliente()), mapper.obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventoPrivadoQuery> consultarPorSedeYRangoFechas(Long idSede, LocalDate inicio, LocalDate fin, Pageable pageable) {
        sedeScope.validarAcceso(idSede);
        return eventoRepository.findBySedeAndFechasBetween(idSede, inicio, fin, pageable)
                .map(e -> mapper.toQuery(e, mapper.obtenerCliente(e.getIdCliente()), mapper.obtenerTurno(e.getIdTurno()), false));
    }

    @Override
    @Transactional(readOnly = true)
    public EventoPrivadoQuery consultarPorId(Long idEvento) {
        EventoPrivado e = mapper.obtenerEvento(idEvento);
        accesoValidator.validarAccesoAlEvento(e.getIdCliente(), e.getIdSede());
        return mapper.toQuery(e, mapper.obtenerCliente(e.getIdCliente()), mapper.obtenerTurno(e.getIdTurno()), true);
    }
}
