package com.playzone.pems.interfaces.rest.notificacion;

import com.playzone.pems.application.notificacion.port.in.MarcarNotificacionLeidaUseCase;
import com.playzone.pems.application.notificacion.port.in.ObtenerNotificacionesUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.notificacion.mapper.NotificacionResponseMapper;
import com.playzone.pems.interfaces.rest.notificacion.response.ConteoNoLeidasResponse;
import com.playzone.pems.interfaces.rest.notificacion.response.NotificacionResponse;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notificaciones/admin/me")
@RequiredArgsConstructor
public class NotificacionAdminController {

    private final ObtenerNotificacionesUseCase   obtenerUseCase;
    private final MarcarNotificacionLeidaUseCase marcarUseCase;
    private final NotificacionResponseMapper     mapper;
    private final SupabaseAuthFacade             supabaseAuthFacade;

    @GetMapping("/feed")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PagedResponse<NotificacionResponse>>> feed(
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0")     int    page,
            @RequestParam(defaultValue = "20")    int    size) {

        UUID usuarioId = resolverUsuarioId();
        Page<NotificacionResponse> result = obtenerUseCase
                .feedUsuario(usuarioId, soloNoLeidas,
                        PageRequest.of(page, size, Sort.by("fechaCreacion").descending()))
                .map(mapper::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(result)));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ConteoNoLeidasResponse>> count() {
        UUID usuarioId = resolverUsuarioId();
        long total = obtenerUseCase.contarNoLeidasUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok(ConteoNoLeidasResponse.builder().count(total).build()));
    }

    @PatchMapping("/{id}/leida")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> marcarLeida(@PathVariable Long id) {
        UUID usuarioId = resolverUsuarioId();
        marcarUseCase.marcarLeidaUsuario(id, usuarioId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/leidas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> marcarTodasLeidas() {
        UUID usuarioId = resolverUsuarioId();
        marcarUseCase.marcarTodasLeidasUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    private UUID resolverUsuarioId() {
        return supabaseAuthFacade.usuarioActualId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }
}
