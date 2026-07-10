package com.playzone.pems.interfaces.rest.finanzas;

import com.playzone.pems.application.finanzas.dto.command.AnularGastoOperativoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarGastoOperativoCommand;
import com.playzone.pems.application.finanzas.dto.query.GastoOperativoQuery;
import com.playzone.pems.application.finanzas.port.in.GestionarGastoOperativoUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.finanzas.request.AnularRegistroRequest;
import com.playzone.pems.interfaces.rest.finanzas.request.RegistrarGastoOperativoRequest;
import com.playzone.pems.interfaces.rest.finanzas.response.GastoOperativoResponse;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/gastos-operativos")
@RequiredArgsConstructor
public class GastoOperativoController {

    private final GestionarGastoOperativoUseCase useCase;
    private final SupabaseAuthFacade             supabaseAuthFacade;
    private final SedeScopeValidator             sedeScope;

    @GetMapping("/sedes/{idSede}/fecha")
    @PreAuthorize("hasAuthority('egreso.ver')")
    public ResponseEntity<ApiResponse<List<GastoOperativoResponse>>> listarPorFecha(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        sedeScope.validarAcceso(idSede);
        List<GastoOperativoResponse> body = useCase.listarPorFecha(idSede, fecha)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/sedes/{idSede}/rango")
    @PreAuthorize("hasAuthority('egreso.ver')")
    public ResponseEntity<ApiResponse<List<GastoOperativoResponse>>> listarPorRango(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        sedeScope.validarAcceso(idSede);
        List<GastoOperativoResponse> body = useCase.listarPorRango(idSede, inicio, fin)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @PostMapping("/sedes/{idSede}")
    @PreAuthorize("hasAuthority('egreso.crear')")
    public ResponseEntity<ApiResponse<GastoOperativoResponse>> registrar(
            @PathVariable Long idSede,
            @Valid @RequestBody RegistrarGastoOperativoRequest request) {
        sedeScope.validarAcceso(idSede);
        GastoOperativoQuery query = useCase.registrar(RegistrarGastoOperativoCommand.builder()
                .idSede(idSede)
                .fecha(request.getFecha())
                .descripcion(request.getDescripcion())
                .monto(request.getMonto())
                .comprobanteUrl(request.getComprobanteUrl())
                .idUsuarioRegistra(supabaseAuthFacade.usuarioActualId().orElseThrow())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(query)));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('egreso.eliminar')")
    public ResponseEntity<ApiResponse<GastoOperativoResponse>> anular(
            @PathVariable Long id,
            @Valid @RequestBody AnularRegistroRequest request) {
        GastoOperativoQuery query = useCase.anular(AnularGastoOperativoCommand.builder()
                .id(id)
                .motivo(request.getMotivo())
                .idUsuarioAnula(supabaseAuthFacade.usuarioActualId()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado")))
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(query)));
    }

    private GastoOperativoResponse toResponse(GastoOperativoQuery q) {
        return GastoOperativoResponse.builder()
                .id(q.getId())
                .idSede(q.getIdSede())
                .fecha(q.getFecha())
                .descripcion(q.getDescripcion())
                .monto(q.getMonto())
                .comprobanteUrl(q.getComprobanteUrl())
                .naturaleza(q.getNaturaleza())
                .idGastoAnulado(q.getIdGastoAnulado())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }
}
