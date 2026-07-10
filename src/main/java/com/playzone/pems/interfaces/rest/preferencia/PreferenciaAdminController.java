package com.playzone.pems.interfaces.rest.preferencia;

import com.playzone.pems.application.preferencia.dto.command.ActualizarPreferenciaAdminCommand;
import com.playzone.pems.application.preferencia.dto.response.PreferenciaAdminResponse;
import com.playzone.pems.application.preferencia.port.in.ActualizarPreferenciaAdminUseCase;
import com.playzone.pems.application.preferencia.port.in.ObtenerPreferenciaAdminUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthContext;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.preferencia.request.ActualizarPreferenciaAdminRequest;
import com.playzone.pems.shared.exception.UnauthorizedException;
import com.playzone.pems.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferencias/admin")
@RequiredArgsConstructor
public class PreferenciaAdminController {

    private final ObtenerPreferenciaAdminUseCase  obtenerUseCase;
    private final ActualizarPreferenciaAdminUseCase actualizarUseCase;
    private final SupabaseAuthFacade               authFacade;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PreferenciaAdminResponse>> obtener() {
        UUID userId = userId();
        return ResponseEntity.ok(ApiResponse.ok(obtenerUseCase.obtener(userId)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<PreferenciaAdminResponse>> actualizar(
            @RequestBody ActualizarPreferenciaAdminRequest request) {
        UUID userId = userId();
        ActualizarPreferenciaAdminCommand command = toCommand(request);
        return ResponseEntity.ok(ApiResponse.ok(actualizarUseCase.actualizar(userId, command)));
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<PreferenciaAdminResponse>> parchear(
            @RequestBody Map<String, Object> patch) {
        UUID userId = userId();
        return ResponseEntity.ok(ApiResponse.ok(actualizarUseCase.parchear(userId, patch)));
    }

    @PostMapping("/reset")
    public ResponseEntity<ApiResponse<PreferenciaAdminResponse>> resetear() {
        UUID userId = userId();
        return ResponseEntity.ok(ApiResponse.ok(actualizarUseCase.resetear(userId)));
    }

    private UUID userId() {
        return authFacade.contextoActual()
                .map(SupabaseAuthContext::userId)
                .orElseThrow(() -> new UnauthorizedException("No autenticado"));
    }

    private ActualizarPreferenciaAdminCommand toCommand(ActualizarPreferenciaAdminRequest r) {
        return ActualizarPreferenciaAdminCommand.builder()
                .tema(r.getTema())
                .tipografia(r.getTipografia())
                .tamanioFuente(r.getTamanioFuente())
                .sonidoNotificaciones(r.isSonidoNotificaciones())
                .notificacionesPush(r.isNotificacionesPush())
                .notificacionesEmail(r.isNotificacionesEmail())
                .notificacionesVisuales(r.isNotificacionesVisuales())
                .badgesDinamicos(r.isBadgesDinamicos())
                .build();
    }
}
