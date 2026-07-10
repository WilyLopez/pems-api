package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.finanzas.dto.query.*;
import com.playzone.pems.application.finanzas.port.in.ConsultarResumenFinancieroUseCase;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.evento.query.IngresosPorDia;
import com.playzone.pems.domain.evento.query.ReservasPorDia;
import com.playzone.pems.domain.finanzas.query.GastosPorDia;
import com.playzone.pems.domain.finanzas.repository.GastoEventoPrivadoRepository;
import com.playzone.pems.domain.finanzas.repository.GastoOperativoDiarioRepository;
import com.playzone.pems.domain.finanzas.repository.RegistroEgresoRepository;
import com.playzone.pems.domain.finanzas.repository.TipoEgresoRepository;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResumenFinancieroService implements ConsultarResumenFinancieroUseCase {

    private final ReservaPublicaRepository       reservaPublicaRepository;
    private final EventoPrivadoRepository        eventoPrivadoRepository;
    private final RegistroEgresoRepository       registroEgresoRepository;
    private final ClientePerfilRepository        clientePerfilRepository;
    private final GastoEventoPrivadoRepository   gastoEventoRepository;
    private final GastoOperativoDiarioRepository gastoOperativoRepository;
    private final TipoEgresoRepository           tipoEgresoRepository;
    private final VentaPagoRepository            ventaPagoRepository;
    private final SedeScopeValidator             sedeScope;

    @Override
    public ResumenFinancieroQuery resumenMensual(Long idSede, int anio, int mes) {
        BigDecimal ingresoReservas = reservaPublicaRepository.sumIngresosBySedeAndPeriodo(idSede, anio, mes);
        BigDecimal adelantoEventos = eventoPrivadoRepository.sumAdelantosBySedeAndPeriodo(idSede, anio, mes);
        BigDecimal ingresoOtros    = BigDecimal.ZERO;
        BigDecimal ingresoGeneral  = ingresoReservas.add(adelantoEventos).add(ingresoOtros);

        BigDecimal egresoGeneral   = registroEgresoRepository.sumMontoBySedeAndPeriodo(idSede, anio, mes);
        BigDecimal egresoOperativo = gastoOperativoRepository.sumMontoBySedeAndPeriodo(idSede, anio, mes);

        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin    = YearMonth.of(anio, mes).atEndOfMonth();
        List<Long> idsEventos = eventoPrivadoRepository
                .findBySedeAndFechaBetween(idSede, inicio, fin)
                .stream().map(EventoPrivado::getId).toList();
        Map<Long, BigDecimal> montosPorEvento = gastoEventoRepository.sumMontoByEventoIds(idsEventos);
        BigDecimal egresoEventos = montosPorEvento.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal egresoNeto   = egresoGeneral.add(egresoOperativo).add(egresoEventos);
        BigDecimal utilidadNeta = ingresoGeneral.subtract(egresoNeto);

        Map<String, BigDecimal> montoPorTipo = registroEgresoRepository.sumMontoAgrupadoPorTipo(idSede, anio, mes);
        List<DesgloseTipoEgresoQuery> desglose = tipoEgresoRepository.findAllActivos().stream()
                .filter(tipo -> montoPorTipo.containsKey(tipo.getCodigo()))
                .map(tipo -> DesgloseTipoEgresoQuery.builder()
                        .nombreTipo(tipo.getNombre())
                        .categoria(tipo.getCategoria())
                        .totalMonto(montoPorTipo.get(tipo.getCodigo()))
                        .build())
                .toList();

        return ResumenFinancieroQuery.builder()
                .anio(anio)
                .mes(mes)
                .totalIngresoReservas(ingresoReservas)
                .totalAdelantoEventos(adelantoEventos)
                .totalIngresoOtros(ingresoOtros)
                .totalIngresoGeneral(ingresoGeneral)
                .totalEgresoGeneral(egresoGeneral)
                .totalEgresoEventos(egresoEventos)
                .totalEgresoOperativo(egresoOperativo)
                .totalEgresoNeto(egresoNeto)
                .utilidadNeta(utilidadNeta)
                .desglosePorTipoEgreso(desglose)
                .build();
    }

    @Override
    public ResumenEventoFinancieroQuery resumenEvento(Long idEvento) {
        EventoPrivado evento = eventoPrivadoRepository.findById(idEvento)
                .orElseThrow(() -> new ResourceNotFoundException("Evento privado no encontrado."));
        sedeScope.validarAcceso(evento.getIdSede());

        BigDecimal ingresoContrato = evento.getPrecioContrato() != null
                ? evento.getPrecioContrato() : BigDecimal.ZERO;
        BigDecimal montoAdelanto   = evento.getMontoAdelanto() != null
                ? evento.getMontoAdelanto() : BigDecimal.ZERO;

        BigDecimal gastosAdicionales = gastoEventoRepository.sumMontoByEvento(idEvento);
        BigDecimal totalGastos       = gastosAdicionales;
        BigDecimal utilidadBruta     = ingresoContrato.subtract(totalGastos);

        return ResumenEventoFinancieroQuery.builder()
                .idEvento(evento.getId())
                .tipoEvento(evento.getTipoEvento())
                .nombreCliente(clientePerfilRepository.buscarPorId(evento.getIdCliente())
                        .map(ClientePerfil::nombreCompleto)
                        .orElse(null))
                .fechaEvento(evento.getFechaEvento())
                .ingresoContrato(ingresoContrato)
                .montoAdelanto(montoAdelanto)
                .totalGastosAdicionales(gastosAdicionales)
                .totalGastos(totalGastos)
                .utilidadBruta(utilidadBruta)
                .build();
    }

    @Override
    public List<ResumenDiarioFinancieroQuery> resumenDiario(Long idSede, LocalDate inicio, LocalDate fin) {
        Map<LocalDate, BigDecimal> ingresosMap = reservaPublicaRepository.sumIngresosAgrupadoPorDia(idSede, inicio, fin).stream()
                .collect(Collectors.toMap(IngresosPorDia::fecha, IngresosPorDia::monto));

        Map<LocalDate, BigDecimal> gastosMap = gastoOperativoRepository.sumMontoAgrupadoPorDia(idSede, inicio, fin).stream()
                .collect(Collectors.toMap(GastosPorDia::fecha, GastosPorDia::monto));

        Map<LocalDate, Long> reservasMap = reservaPublicaRepository.countAgrupadoPorDia(idSede, inicio, fin).stream()
                .collect(Collectors.toMap(ReservasPorDia::fecha, ReservasPorDia::cantidad));

        List<ResumenDiarioFinancieroQuery> resultado = new ArrayList<>();
        LocalDate cursor = inicio;
        while (!cursor.isAfter(fin)) {
            BigDecimal ingresoReservas = ingresosMap.getOrDefault(cursor, BigDecimal.ZERO);
            BigDecimal gastoOperativo  = gastosMap.getOrDefault(cursor, BigDecimal.ZERO);
            long cantidadReservas      = reservasMap.getOrDefault(cursor, 0L);

            BigDecimal utilidadDia     = ingresoReservas.subtract(gastoOperativo);
            BigDecimal ticketPromedio  = cantidadReservas > 0
                    ? ingresoReservas.divide(BigDecimal.valueOf(cantidadReservas), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            resultado.add(ResumenDiarioFinancieroQuery.builder()
                    .fecha(cursor)
                    .ingresoReservas(ingresoReservas)
                    .gastoOperativo(gastoOperativo)
                    .utilidadDia(utilidadDia)
                    .cantidadReservas((int) cantidadReservas)
                    .ticketPromedio(ticketPromedio)
                    .build());
            cursor = cursor.plusDays(1);
        }
        return resultado;
    }

    @Override
    public ResumenRangoQuery resumenPorRango(Long idSede, LocalDate inicio, LocalDate fin) {
        BigDecimal ingresoReservas = reservaPublicaRepository.sumIngresosBySedeAndRango(idSede, inicio, fin);
        BigDecimal egresoGeneral   = registroEgresoRepository.sumMontoBySedeAndRango(idSede, inicio, fin);
        BigDecimal egresoOperativo = gastoOperativoRepository.sumMontoBySedeAndRango(idSede, inicio, fin);
        BigDecimal egresoNeto      = egresoGeneral.add(egresoOperativo);
        BigDecimal utilidadNeta    = ingresoReservas.subtract(egresoNeto);
        long cantidadReservas      = reservaPublicaRepository.countConfirmadasBySedeAndRango(idSede, inicio, fin);
        return ResumenRangoQuery.builder()
                .inicio(inicio)
                .fin(fin)
                .totalIngresoReservas(ingresoReservas)
                .totalEgresoGeneral(egresoGeneral)
                .totalEgresoOperativo(egresoOperativo)
                .totalEgresoNeto(egresoNeto)
                .utilidadNeta(utilidadNeta)
                .cantidadReservas(cantidadReservas)
                .build();
    }

    @Override
    public MetricasReservasQuery metricasReservas(Long idSede, int anio, int mes) {
        long confirmadas   = reservaPublicaRepository.countConfirmadasBySedeAndPeriodo(idSede, anio, mes);
        long canceladas    = reservaPublicaRepository.countCanceladasBySedeAndPeriodo(idSede, anio, mes);
        long completadas   = reservaPublicaRepository.countCompletadasBySedeAndPeriodo(idSede, anio, mes);
        BigDecimal ingreso = reservaPublicaRepository.sumIngresosBySedeAndPeriodo(idSede, anio, mes);
        BigDecimal ticket  = reservaPublicaRepository.avgTicketBySedeAndPeriodo(idSede, anio, mes);
        BigDecimal efectivo = ventaPagoRepository
                .sumValidadosBySedeAndPeriodoAndMedioPago(idSede, anio, mes, "EFECTIVO");
        BigDecimal yape = ventaPagoRepository
                .sumValidadosBySedeAndPeriodoAndMedioPago(idSede, anio, mes, "YAPE");
        return MetricasReservasQuery.builder()
                .anio(anio)
                .mes(mes)
                .totalConfirmadas(confirmadas)
                .totalCanceladas(canceladas)
                .totalCompletadas(completadas)
                .ingresoTotal(ingreso)
                .ticketPromedio(ticket)
                .ingresoEfectivo(efectivo)
                .ingresoYape(yape)
                .build();
    }
}
