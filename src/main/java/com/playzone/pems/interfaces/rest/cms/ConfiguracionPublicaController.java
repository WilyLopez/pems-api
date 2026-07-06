package com.playzone.pems.interfaces.rest.cms;

import com.playzone.pems.application.cms.dto.query.ConfiguracionPublicaQuery;
import com.playzone.pems.application.cms.port.in.GestionarConfiguracionPublicaUseCase;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/cms/configuracion")
@RequiredArgsConstructor
public class ConfiguracionPublicaController {

    private final GestionarConfiguracionPublicaUseCase configUseCase;

 
    @GetMapping("/publica")
    public ResponseEntity<ApiResponse<ConfiguracionPublicaResponse>> obtenerPublica() {
        return ResponseEntity.ok(ApiResponse.ok(
                ConfiguracionPublicaResponse.from(configUseCase.obtener())));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('configuracion.editar')")
    public ResponseEntity<ApiResponse<ConfiguracionPublicaResponse>> obtener() {
        return ResponseEntity.ok(ApiResponse.ok(
                ConfiguracionPublicaResponse.from(configUseCase.obtener())));
    }

    @PutMapping
    @PreAuthorize("hasAuthority('configuracion.editar')")
    public ResponseEntity<ApiResponse<ConfiguracionPublicaResponse>> actualizar(
            @Valid @RequestBody ActualizarConfiguracionRequest request) {
        ConfiguracionPublicaResponse response = ConfiguracionPublicaResponse.from(
                configUseCase.actualizar(new GestionarConfiguracionPublicaUseCase.ActualizarCommand(
                        request.getNombreNegocio(),
                        request.getSlogan(),
                        request.getLogoUrl(),
                        request.getFaviconUrl(),
                        request.getTelefono(),
                        request.getTelefonoSecundario(),
                        request.getWhatsapp(),
                        request.getCorreo(),
                        request.getCorreoSecundario(),
                        request.getFacebookUrl(),
                        request.getInstagramUrl(),
                        request.getTiktokUrl(),
                        request.getYoutubeUrl(),
                        request.getCopyrightTexto(),
                        request.getMetricasNegocio(),
                        request.isMantenimientoActivo(),
                        request.getMensajeMantenimiento(),
                        request.getLogoSecundarioUrl(),
                        request.getMascota1Url(),
                        request.getMascota2Url())));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @Getter
    @NoArgsConstructor
    public static class ActualizarConfiguracionRequest {
        @NotBlank private String  nombreNegocio;
        private String  slogan;
        private String  logoUrl;
        private String  faviconUrl;
        private String  logoSecundarioUrl;
        private String  mascota1Url;
        private String  mascota2Url;
        private String  telefono;
        private String  telefonoSecundario;
        private String  whatsapp;
        private String  correo;
        private String  correoSecundario;
        private String  facebookUrl;
        private String  instagramUrl;
        private String  tiktokUrl;
        private String  youtubeUrl;
        private String  copyrightTexto;
        private String  metricasNegocio;
        private boolean mantenimientoActivo;
        private String  mensajeMantenimiento;
    }

    @Getter
    @Builder
    public static class ConfiguracionPublicaResponse {
        private String        nombreNegocio;
        private String        slogan;
        private String        logoUrl;
        private String        faviconUrl;
        private String        logoSecundarioUrl;
        private String        mascota1Url;
        private String        mascota2Url;
        private String        telefono;
        private String        telefonoSecundario;
        private String        whatsapp;
        private String        correo;
        private String        correoSecundario;
        private String        facebookUrl;
        private String        instagramUrl;
        private String        tiktokUrl;
        private String        youtubeUrl;
        private String        copyrightTexto;
        private String        metricasNegocio;
        private boolean       mantenimientoActivo;
        private String        mensajeMantenimiento;
        private OffsetDateTime updatedAt;

        public static ConfiguracionPublicaResponse from(ConfiguracionPublicaQuery q) {
            return ConfiguracionPublicaResponse.builder()
                    .nombreNegocio(q.getNombreNegocio())
                    .slogan(q.getSlogan())
                    .logoUrl(q.getLogoPath())
                    .faviconUrl(q.getFaviconPath())
                    .logoSecundarioUrl(q.getLogoSecundarioPath())
                    .mascota1Url(q.getMascota1Path())
                    .mascota2Url(q.getMascota2Path())
                    .telefono(q.getTelefono())
                    .telefonoSecundario(q.getTelefonoSecundario())
                    .whatsapp(q.getWhatsapp())
                    .correo(q.getCorreo())
                    .correoSecundario(q.getCorreoSecundario())
                    .facebookUrl(q.getFacebookUrl())
                    .instagramUrl(q.getInstagramUrl())
                    .tiktokUrl(q.getTiktokUrl())
                    .youtubeUrl(q.getYoutubeUrl())
                    .copyrightTexto(q.getCopyrightTexto())
                    .metricasNegocio(q.getMetricasNegocio())
                    .mantenimientoActivo(q.isEsMantenimientoActivo())
                    .mensajeMantenimiento(q.getMensajeMantenimiento())
                    .updatedAt(q.getUpdatedAt())
                    .build();
        }
    }
}
