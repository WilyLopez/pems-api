package com.playzone.pems.interfaces.rest.cms;

import com.playzone.pems.application.cms.port.in.GestionarMensajeContactoUseCase;
import com.playzone.pems.domain.cms.model.MensajeContacto;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.ratelimit.RateLimited;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contacto")
@RequiredArgsConstructor
public class MensajeContactoController {

    private final GestionarMensajeContactoUseCase contactUseCase;
    private final SupabaseAuthFacade              supabaseAuthFacade;

    // ── Público ──────────────────────────────────────────────────────────

    @PostMapping
    @RateLimited(requests = 3, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<MensajeContactoResponse>> registrar(
            @Valid @RequestBody RegistrarMensajeRequest request,
            HttpServletRequest servletRequest) {

        String ip = servletRequest.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = servletRequest.getRemoteAddr();
        }
        String userAgent = servletRequest.getHeader("User-Agent");

        MensajeContacto mensaje = contactUseCase.registrar(
                GestionarMensajeContactoUseCase.RegistrarCommand.builder()
                        .nombre(request.getNombre())
                        .correo(request.getCorreo())
                        .telefono(request.getTelefono())
                        .asunto(request.getAsunto())
                        .mensaje(request.getMensaje())
                        .ipOrigen(ip)
                        .userAgent(userAgent)
                        .build()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(MensajeContactoResponse.from(mensaje)));
    }

    // ── Admin ─────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('sitio.contacto')")
    public ResponseEntity<ApiResponse<PagedResponse<MensajeContactoResponse>>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String dir) {

        var pageable = PageRequest.of(page, size,
                "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC, sort);

        PagedResponse<MensajeContactoResponse> response = PagedResponse.of(
                contactUseCase.listar(estado, pageable).map(MensajeContactoResponse::from)
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sitio.contacto')")
    public ResponseEntity<ApiResponse<MensajeContactoResponse>> obtener(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                MensajeContactoResponse.from(contactUseCase.obtener(id))
        ));
    }

    @PostMapping("/{id}/responder")
    @PreAuthorize("hasAuthority('sitio.contacto')")
    public ResponseEntity<ApiResponse<MensajeContactoResponse>> responder(
            @PathVariable Long id,
            @Valid @RequestBody ResponderMensajeRequest request) {

        UUID adminId = supabaseAuthFacade.usuarioActualId().orElseThrow();

        MensajeContacto mensaje = contactUseCase.responder(
                GestionarMensajeContactoUseCase.ResponderCommand.builder()
                        .idMensaje(id)
                        .respuesta(request.getRespuesta())
                        .idUsuarioAdmin(adminId)
                        .build()
        );

        return ResponseEntity.ok(ApiResponse.ok(MensajeContactoResponse.from(mensaje)));
    }

    @PatchMapping("/{id}/leido")
    @PreAuthorize("hasAuthority('sitio.contacto')")
    public ResponseEntity<ApiResponse<MensajeContactoResponse>> marcarComoLeido(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                MensajeContactoResponse.from(contactUseCase.marcarComoLeido(id))
        ));
    }

    @PatchMapping("/{id}/spam")
    @PreAuthorize("hasAuthority('sitio.contacto')")
    public ResponseEntity<ApiResponse<MensajeContactoResponse>> marcarComoSpam(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                MensajeContactoResponse.from(contactUseCase.marcarComoSpam(id))
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sitio.contacto')")
    public ResponseEntity<ApiResponse<Void>> eliminar(
            @PathVariable Long id) {
        contactUseCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // ── Request / Response DTOs ───────────────────────────────────────────

    @Getter
    @NoArgsConstructor
    public static class RegistrarMensajeRequest {
        @NotBlank(message = "El nombre es obligatorio")
        private String nombre;

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe proporcionar un correo electrónico válido")
        private String correo;

        private String telefono;
        private String asunto;

        @NotBlank(message = "El mensaje es obligatorio")
        private String mensaje;
    }

    @Getter
    @NoArgsConstructor
    public static class ResponderMensajeRequest {
        @NotBlank(message = "La respuesta no puede estar vacía")
        private String respuesta;
    }

    @Getter
    @Builder
    public static class MensajeContactoResponse {
        private Long           id;
        private String         nombre;
        private String         correo;
        private String         telefono;
        private String         asunto;
        private String         mensaje;
        private String         estado;
        private String         respuesta;
        private UUID           respondidoPor;
        private OffsetDateTime respondidoAt;
        private String         ipOrigen;
        private String         userAgent;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;

        public static MensajeContactoResponse from(MensajeContacto m) {
            if (m == null) return null;
            return MensajeContactoResponse.builder()
                    .id(m.getId())
                    .nombre(m.getNombre())
                    .correo(m.getCorreo())
                    .telefono(m.getTelefono())
                    .asunto(m.getAsunto())
                    .mensaje(m.getMensaje())
                    .estado(m.getEstado())
                    .respuesta(m.getRespuesta())
                    .respondidoPor(m.getRespondidoPor())
                    .respondidoAt(m.getRespondidoAt())
                    .ipOrigen(m.getIpOrigen())
                    .userAgent(m.getUserAgent())
                    .createdAt(m.getCreatedAt())
                    .updatedAt(m.getUpdatedAt())
                    .build();
        }
    }
}
