package com.playzone.pems.interfaces.rest.configuracion;

import com.playzone.pems.application.configuracion.port.in.GestionarConfiguracionUseCase;
import com.playzone.pems.domain.configuracion.model.ConfiguracionGlobal;
import com.playzone.pems.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/configuracion")
@RequiredArgsConstructor
public class ConfiguracionController {

    private final GestionarConfiguracionUseCase gestionarUseCase;

    @GetMapping("/publica")
    public ResponseEntity<ApiResponse<Map<String, String>>> obtenerPublicas() {
        return ResponseEntity.ok(ApiResponse.ok(gestionarUseCase.obtenerPublicas()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('configuracion.editar')")
    public ResponseEntity<ApiResponse<List<ConfiguracionResponse>>> listar() {
        List<ConfiguracionResponse> lista = gestionarUseCase.listar().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(lista));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('configuracion.editar')")
    public ResponseEntity<ApiResponse<List<ConfiguracionResponse>>> actualizar(
            @RequestBody Map<String, String> cambios) {
        List<ConfiguracionResponse> actualizados = gestionarUseCase.actualizar(cambios).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(actualizados));
    }

    private ConfiguracionResponse toResponse(ConfiguracionGlobal c) {
        return new ConfiguracionResponse(
                c.getClave(), c.getValor(),
                c.getDescripcion(), c.getTipo(), c.isEsSecreto(), c.getUpdatedAt());
    }

    public record ConfiguracionResponse(
            String         clave,
            String         valor,
            String         descripcion,
            String         tipo,
            boolean        esSecreto,
            OffsetDateTime updatedAt
    ) {}
}
