package com.playzone.pems.domain.cms.model;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ConfiguracionPublica {

    private Long          id;

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

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID          updatedBy;
}
