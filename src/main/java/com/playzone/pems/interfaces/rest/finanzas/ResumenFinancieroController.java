package com.playzone.pems.interfaces.rest.finanzas;

import com.playzone.pems.application.finanzas.dto.query.MetricasReservasQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenDiarioFinancieroQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenEventoFinancieroQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenFinancieroQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenRangoQuery;
import com.playzone.pems.application.finanzas.port.in.ConsultarResumenFinancieroUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.interfaces.rest.finanzas.response.MetricasReservasResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.ResumenDiarioResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.ResumenEventoFinancieroResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.ResumenFinancieroResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.ResumenRangoResponse;
import com.playzone.pems.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/finanzas")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('finanzas.reportes')")
public class ResumenFinancieroController {

    private final ConsultarResumenFinancieroUseCase useCase;
    private final SedeScopeValidator                sedeScope;

    @GetMapping("/sedes/{idSede}/resumen-mensual")
    public ResponseEntity<ApiResponse<ResumenFinancieroResponse>> resumenMensual(
            @PathVariable Long idSede,
            @RequestParam int anio,
            @RequestParam int mes) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.resumenMensual(idSede, anio, mes))));
    }

    @GetMapping("/eventos/{idEvento}/resumen")
    public ResponseEntity<ApiResponse<ResumenEventoFinancieroResponse>> resumenEvento(
            @PathVariable Long idEvento) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.resumenEvento(idEvento))));
    }

    @GetMapping("/sedes/{idSede}/resumen-diario")
    public ResponseEntity<ApiResponse<List<ResumenDiarioResponse>>> resumenDiario(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        sedeScope.validarAcceso(idSede);
        List<ResumenDiarioResponse> body = useCase.resumenDiario(idSede, inicio, fin)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/sedes/{idSede}/resumen-rango")
    public ResponseEntity<ApiResponse<ResumenRangoResponse>> resumenPorRango(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.resumenPorRango(idSede, inicio, fin))));
    }

    @GetMapping("/sedes/{idSede}/metricas-reservas")
    public ResponseEntity<ApiResponse<MetricasReservasResponse>> metricasReservas(
            @PathVariable Long idSede,
            @RequestParam int anio,
            @RequestParam int mes) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.metricasReservas(idSede, anio, mes))));
    }

    private ResumenFinancieroResponse toResponse(ResumenFinancieroQuery q) {
        List<ResumenFinancieroResponse.DesgloseTipoEgreso> desglose = q.getDesglosePorTipoEgreso()
                .stream()
                .map(d -> ResumenFinancieroResponse.DesgloseTipoEgreso.builder()
                        .nombreTipo(d.getNombreTipo())
                        .categoria(d.getCategoria())
                        .totalMonto(d.getTotalMonto())
                        .build())
                .toList();
        return ResumenFinancieroResponse.builder()
                .anio(q.getAnio())
                .mes(q.getMes())
                .totalIngresoReservas(q.getTotalIngresoReservas())
                .totalAdelantoEventos(q.getTotalAdelantoEventos())
                .totalIngresoOtros(q.getTotalIngresoOtros())
                .totalIngresoGeneral(q.getTotalIngresoGeneral())
                .totalEgresoGeneral(q.getTotalEgresoGeneral())
                .totalEgresoEventos(q.getTotalEgresoEventos())
                .totalEgresoOperativo(q.getTotalEgresoOperativo())
                .totalEgresoNeto(q.getTotalEgresoNeto())
                .utilidadNeta(q.getUtilidadNeta())
                .desglosePorTipoEgreso(desglose)
                .build();
    }

    private ResumenRangoResponse toResponse(ResumenRangoQuery q) {
        return ResumenRangoResponse.builder()
                .inicio(q.getInicio())
                .fin(q.getFin())
                .totalIngresoReservas(q.getTotalIngresoReservas())
                .totalEgresoGeneral(q.getTotalEgresoGeneral())
                .totalEgresoOperativo(q.getTotalEgresoOperativo())
                .totalEgresoNeto(q.getTotalEgresoNeto())
                .utilidadNeta(q.getUtilidadNeta())
                .cantidadReservas(q.getCantidadReservas())
                .build();
    }

    private MetricasReservasResponse toResponse(MetricasReservasQuery q) {
        return MetricasReservasResponse.builder()
                .anio(q.getAnio())
                .mes(q.getMes())
                .totalConfirmadas(q.getTotalConfirmadas())
                .totalCanceladas(q.getTotalCanceladas())
                .totalCompletadas(q.getTotalCompletadas())
                .ingresoTotal(q.getIngresoTotal())
                .ticketPromedio(q.getTicketPromedio())
                .ingresoEfectivo(q.getIngresoEfectivo())
                .ingresoYape(q.getIngresoYape())
                .build();
    }

    private ResumenEventoFinancieroResponse toResponse(ResumenEventoFinancieroQuery q) {
        return ResumenEventoFinancieroResponse.builder()
                .idEvento(q.getIdEvento())
                .tipoEvento(q.getTipoEvento())
                .nombreCliente(q.getNombreCliente())
                .fechaEvento(q.getFechaEvento())
                .ingresoContrato(q.getIngresoContrato())
                .montoAdelanto(q.getMontoAdelanto())
                .totalGastosAdicionales(q.getTotalGastosAdicionales())
                .totalGastos(q.getTotalGastos())
                .utilidadBruta(q.getUtilidadBruta())
                .build();
    }

    private ResumenDiarioResponse toResponse(ResumenDiarioFinancieroQuery q) {
        return ResumenDiarioResponse.builder()
                .fecha(q.getFecha())
                .ingresoReservas(q.getIngresoReservas())
                .gastoOperativo(q.getGastoOperativo())
                .utilidadDia(q.getUtilidadDia())
                .cantidadReservas(q.getCantidadReservas())
                .ticketPromedio(q.getTicketPromedio())
                .build();
    }
}
