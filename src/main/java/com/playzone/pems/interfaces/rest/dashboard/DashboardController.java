package com.playzone.pems.interfaces.rest.dashboard;

import com.playzone.pems.application.dashboard.dto.query.DashboardAdminQuery;
import com.playzone.pems.application.dashboard.port.in.ConsultarDashboardAdminUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.interfaces.rest.dashboard.response.DashboardAdminResponse;
import com.playzone.pems.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final ConsultarDashboardAdminUseCase useCase;
    private final SedeScopeValidator             sedeScope;

    @GetMapping("/sedes/{idSede}/resumen")
    @PreAuthorize("hasAuthority('dashboard.ver')")
    public ResponseEntity<ApiResponse<DashboardAdminResponse>> resumen(
            @PathVariable Long idSede) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.obtener(idSede))));
    }

    private DashboardAdminResponse toResponse(DashboardAdminQuery q) {
        return DashboardAdminResponse.builder()
                .fecha(q.getFecha())
                .reservasHoy(q.getReservasHoy())
                .reservasAyer(q.getReservasAyer())
                .ingresosHoy(q.getIngresosHoy())
                .reservasConfirmadas(q.getReservasConfirmadas())
                .pendientesPago(q.getPendientesPago())
                .aforoMaximo(q.getAforoMaximo())
                .plazasDisponibles(q.getPlazasDisponibles())
                .eventosEstaSemana(q.getEventosEstaSemana())
                .solicitudesEventoSinResponder(q.getSolicitudesEventoSinResponder())
                .eventosSaldoPendiente(q.getEventosSaldoPendiente())
                .cajaAbierta(q.isCajaAbierta())
                .yapesPorValidar(q.getYapesPorValidar())
                .reservasHoyDetalle(q.getReservasHoyDetalle().stream()
                        .map(r -> DashboardAdminResponse.AgendaReservaResponse.builder()
                                .numeroTicket(r.getNumeroTicket())
                                .nombreNino(r.getNombreNino())
                                .edadNino(r.getEdadNino())
                                .estado(r.getEstado())
                                .build())
                        .toList())
                .eventosHoyDetalle(q.getEventosHoyDetalle().stream()
                        .map(e -> DashboardAdminResponse.AgendaEventoResponse.builder()
                                .id(e.getId())
                                .tipoEvento(e.getTipoEvento())
                                .nombreCliente(e.getNombreCliente())
                                .turno(e.getTurno())
                                .estado(e.getEstado())
                                .build())
                        .toList())
                .reservasUltimos30Dias(q.getReservasUltimos30Dias().stream()
                        .map(d -> DashboardAdminResponse.ReservasDiaResponse.builder()
                                .fecha(d.getFecha())
                                .cantidad(d.getCantidad())
                                .build())
                        .toList())
                .disponibilidadSemana(q.getDisponibilidadSemana().stream()
                        .map(d -> DashboardAdminResponse.DisponibilidadDiaResponse.builder()
                                .fecha(d.getFecha())
                                .turnoT1Disponible(d.isTurnoT1Disponible())
                                .turnoT2Disponible(d.isTurnoT2Disponible())
                                .totalEventos(d.getTotalEventos())
                                .build())
                        .toList())
                .build();
    }
}
