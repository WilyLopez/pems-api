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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/notificaciones/cliente/me")
@RequiredArgsConstructor
public class NotificacionClienteController {

    private final ObtenerNotificacionesUseCase   obtenerUseCase;
    private final MarcarNotificacionLeidaUseCase marcarUseCase;
    private final NotificacionResponseMapper     mapper;
    private final SupabaseAuthFacade             supabaseAuthFacade;

    @GetMapping("/feed")
    @PreAuthorize("@supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<PagedResponse<NotificacionResponse>>> feed(
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            @RequestParam(defaultValue = "0")     int    page,
            @RequestParam(defaultValue = "20")    int    size) {

        Long clienteId = resolverClienteId();
        Page<NotificacionResponse> result = obtenerUseCase
                .feedCliente(clienteId, soloNoLeidas,
                        PageRequest.of(page, size, Sort.by("fechaCreacion").descending()))
                .map(mapper::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(result)));
    }

    @GetMapping("/count")
    @PreAuthorize("@supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<ConteoNoLeidasResponse>> count() {
        Long clienteId = resolverClienteId();
        long total = obtenerUseCase.contarNoLeidasCliente(clienteId);
        return ResponseEntity.ok(ApiResponse.ok(ConteoNoLeidasResponse.builder().count(total).build()));
    }

    @PatchMapping("/{id}/leida")
    @PreAuthorize("@supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<Void>> marcarLeida(@PathVariable Long id) {
        Long clienteId = resolverClienteId();
        marcarUseCase.marcarLeidaCliente(id, clienteId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/leidas")
    @PreAuthorize("@supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<Void>> marcarTodasLeidas() {
        Long clienteId = resolverClienteId();
        marcarUseCase.marcarTodasLeidasCliente(clienteId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        Long clienteId = resolverClienteId();
        marcarUseCase.eliminarCliente(id, clienteId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    private Long resolverClienteId() {
        return supabaseAuthFacade.clientePerfilId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Perfil de cliente no disponible"));
    }
}
