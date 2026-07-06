package com.playzone.pems.application.cms.port.in;

import com.playzone.pems.application.cms.dto.query.ConfiguracionPublicaQuery;

public interface GestionarConfiguracionPublicaUseCase {
    // Port for managing public website configuration

    record ActualizarCommand(
            String nombreNegocio,
            String slogan,
            String logoUrl,
            String faviconUrl,
            String telefono,
            String telefonoSecundario,
            String whatsapp,
            String correo,
            String correoSecundario,
            String facebookUrl,
            String instagramUrl,
            String tiktokUrl,
            String youtubeUrl,
            String copyrightTexto,
            String metricasNegocio,
            boolean mantenimientoActivo,
            String mensajeMantenimiento,
            String logoSecundarioUrl,
            String mascota1Url,
            String mascota2Url
    ) {}

    ConfiguracionPublicaQuery obtener();

    ConfiguracionPublicaQuery actualizar(ActualizarCommand command);
}
