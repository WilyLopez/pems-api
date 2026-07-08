package com.playzone.pems.interfaces.rest.finanzas;

import com.playzone.pems.application.finanzas.dto.query.GastoEventoQuery;
import com.playzone.pems.application.finanzas.port.in.GestionarGastoEventoUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.interfaces.rest.finanzas.response.GastoEventoResponse;
import com.playzone.pems.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/gastos-evento")
@RequiredArgsConstructor
public class GastoEventoPorSedeController {

    private final GestionarGastoEventoUseCase useCase;
    private final SedeScopeValidator          sedeScope;

    @GetMapping("/sedes/{idSede}/rango")
    @PreAuthorize("hasAuthority('egreso.ver')")
    public ResponseEntity<ApiResponse<List<GastoEventoResponse>>> listarPorRango(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        sedeScope.validarAcceso(idSede);
        if (inicio.isAfter(fin))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "inicio debe ser anterior o igual a fin");
        if (ChronoUnit.DAYS.between(inicio, fin) > 365)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rango máximo: 365 días");
        List<GastoEventoResponse> body = useCase.listarPorSedeYRango(idSede, inicio, fin)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    private GastoEventoResponse toResponse(GastoEventoQuery q) {
        return GastoEventoResponse.builder()
                .id(q.getId())
                .idEventoPrivado(q.getIdEventoPrivado())
                .fechaEvento(q.getFechaEvento())
                .descripcion(q.getDescripcion())
                .monto(q.getMonto())
                .comprobanteUrl(q.getComprobanteUrl())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }
}
