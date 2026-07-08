package com.playzone.pems.interfaces.rest.finanzas;

import com.playzone.pems.application.finanzas.dto.command.RegistrarIngresoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.RegistroIngresoQuery;
import com.playzone.pems.application.finanzas.port.in.RegistrarIngresoUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.finanzas.request.RegistrarIngresoManualRequest;
import com.playzone.pems.interfaces.rest.finanzas.response.RegistroIngresoResponse;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ingresos")
@RequiredArgsConstructor
public class IngresoController {

    private final RegistrarIngresoUseCase useCase;
    private final SupabaseAuthFacade      supabaseAuthFacade;
    private final SedeScopeValidator      sedeScope;

    @GetMapping("/sedes/{idSede}")
    @PreAuthorize("hasAuthority('ingreso.ver')")
    public ResponseEntity<ApiResponse<PagedResponse<RegistroIngresoResponse>>> listar(
            @PathVariable Long idSede,
            @PageableDefault(size = 20) Pageable pageable) {
        sedeScope.validarAcceso(idSede);
        PagedResponse<RegistroIngresoResponse> body = PagedResponse.of(
                useCase.listar(idSede, pageable).map(this::toResponse));
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/sedes/{idSede}/rango")
    @PreAuthorize("hasAuthority('ingreso.ver')")
    public ResponseEntity<ApiResponse<List<RegistroIngresoResponse>>> listarPorRango(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        sedeScope.validarAcceso(idSede);
        if (inicio.isAfter(fin))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inicio debe ser anterior o igual a fin");
        if (ChronoUnit.DAYS.between(inicio, fin) > 365)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango máximo: 365 días");
        List<RegistroIngresoResponse> body = useCase.listarPorRango(idSede, inicio, fin)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @PostMapping("/sedes/{idSede}")
    @PreAuthorize("hasAuthority('ingreso.crear')")
    public ResponseEntity<ApiResponse<RegistroIngresoResponse>> registrar(
            @PathVariable Long idSede,
            @Valid @RequestBody RegistrarIngresoManualRequest request) {
        sedeScope.validarAcceso(idSede);
        RegistroIngresoQuery query = useCase.registrar(RegistrarIngresoManualCommand.builder()
                .tipoIngresoCodigo(request.getTipoIngresoCodigo())
                .idSede(idSede)
                .monto(request.getMonto())
                .fecha(request.getFecha())
                .medioPago(request.getMedioPago())
                .descripcion(request.getDescripcion())
                .idUsuarioRegistra(supabaseAuthFacade.usuarioActualId()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado")))
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(query)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ingreso.eliminar')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    private RegistroIngresoResponse toResponse(RegistroIngresoQuery q) {
        return RegistroIngresoResponse.builder()
                .id(q.getId())
                .tipoIngresoCodigo(q.getTipoIngresoCodigo())
                .idSede(q.getIdSede())
                .idReservaPublica(q.getIdReservaPublica())
                .idEventoPrivado(q.getIdEventoPrivado())
                .monto(q.getMonto())
                .fecha(q.getFecha())
                .medioPago(q.getMedioPago())
                .descripcion(q.getDescripcion())
                .esAutomatico(q.isEsAutomatico())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }
}
