package com.playzone.pems.application.cms.dto.query;

import com.playzone.pems.domain.cms.model.ConfiguracionPublica;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class ConfiguracionPublicaQuery {

    private String        nombreNegocio;
    private String        slogan;
    private String        logoPath;
    private String        faviconPath;
    private String        logoSecundarioPath;
    private String        mascota1Path;
    private String        mascota2Path;
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
    private boolean       esMantenimientoActivo;
    private String        mensajeMantenimiento;
    private OffsetDateTime updatedAt;

    public static ConfiguracionPublicaQuery from(ConfiguracionPublica c) {
        return ConfiguracionPublicaQuery.builder()
                .nombreNegocio(c.getNombreNegocio())
                .slogan(c.getSlogan())
                .logoPath(c.getLogoPath())
                .faviconPath(c.getFaviconPath())
                .logoSecundarioPath(c.getLogoSecundarioPath())
                .mascota1Path(c.getMascota1Path())
                .mascota2Path(c.getMascota2Path())
                .telefono(c.getTelefono())
                .telefonoSecundario(c.getTelefonoSecundario())
                .whatsapp(c.getWhatsapp())
                .correo(c.getCorreo())
                .correoSecundario(c.getCorreoSecundario())
                .facebookUrl(c.getFacebookUrl())
                .instagramUrl(c.getInstagramUrl())
                .tiktokUrl(c.getTiktokUrl())
                .youtubeUrl(c.getYoutubeUrl())
                .copyrightTexto(c.getCopyrightTexto())
                .metricasNegocio(c.getMetricasNegocio())
                .esMantenimientoActivo(c.isEsMantenimientoActivo())
                .mensajeMantenimiento(c.getMensajeMantenimiento())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
