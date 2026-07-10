package com.playzone.pems.interfaces.rest.finanzas;

import com.playzone.pems.application.finanzas.dto.query.DashboardFinancieroQuery;
import com.playzone.pems.application.finanzas.port.in.ConsultarDashboardFinancieroUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.interfaces.rest.finanzas.response.DashboardFinancieroResponse;
import com.playzone.pems.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard-financiero")
@RequiredArgsConstructor
public class DashboardFinancieroController {

    private final ConsultarDashboardFinancieroUseCase useCase;
    private final SedeScopeValidator                  sedeScope;

    @GetMapping("/sedes/{idSede}")
    @PreAuthorize("hasAuthority('finanzas.ver')")
    public ResponseEntity<ApiResponse<DashboardFinancieroResponse>> consultar(
            @PathVariable Long idSede,
            @RequestParam int anio,
            @RequestParam int mes) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.consultar(idSede, anio, mes))));
    }

    private DashboardFinancieroResponse toResponse(DashboardFinancieroQuery q) {
        return DashboardFinancieroResponse.builder()
                .anio(q.getAnio())
                .mes(q.getMes())
                .totalIngresos(q.getTotalIngresos())
                .totalEgresos(q.getTotalEgresos())
                .utilidadNeta(q.getUtilidadNeta())
                .ingresoReservas(q.getIngresoReservas())
                .ingresoAdelantos(q.getIngresoAdelantos())
                .ingresoManual(q.getIngresoManual())
                .egresoFijo(q.getEgresoFijo())
                .egresoVariable(q.getEgresoVariable())
                .egresoEventual(q.getEgresoEventual())
                .reservasConfirmadas(q.getReservasConfirmadas())
                .reservasCanceladas(q.getReservasCanceladas())
                .ticketPromedio(q.getTicketPromedio())
                .saldoPendienteEventos(q.getSaldoPendienteEventos())
                .totalIngresosMesAnterior(q.getTotalIngresosMesAnterior())
                .totalEgresosMesAnterior(q.getTotalEgresosMesAnterior())
                .utilidadMesAnterior(q.getUtilidadMesAnterior())
                .serieDiaria(q.getSerieDiaria().stream()
                        .map(s -> DashboardFinancieroResponse.SerieDiaResponse.builder()
                                .fecha(s.fecha())
                                .ingresos(s.ingresos())
                                .egresos(s.egresos())
                                .build())
                        .toList())
                .build();
    }
}
