package com.playzone.pems.application.cms.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.cms.dto.query.ConfiguracionPublicaQuery;
import com.playzone.pems.application.cms.port.in.GestionarConfiguracionPublicaUseCase;
import com.playzone.pems.domain.cms.model.ConfiguracionPublica;
import com.playzone.pems.domain.cms.repository.ConfiguracionPublicaRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracionPublicaService implements GestionarConfiguracionPublicaUseCase {

    private final ConfiguracionPublicaRepository configRepository;
    private final SupabaseAuthFacade             supabaseAuthFacade;
    private final RegistrarLogUseCase            auditoria;

    @Override
    @Transactional
    public ConfiguracionPublicaQuery obtener() {
        return ConfiguracionPublicaQuery.from(obtenerOAutocrear());
    }

    @Override
    @Transactional
    public ConfiguracionPublicaQuery actualizar(ActualizarCommand cmd) {
        ConfiguracionPublica existente = obtenerOAutocrear();

        ConfiguracionPublica actualizado = existente.toBuilder()
                .nombreNegocio(cmd.nombreNegocio())
                .slogan(cmd.slogan())
                .logoPath(cmd.logoUrl())
                .faviconPath(cmd.faviconUrl())
                .logoSecundarioPath(cmd.logoSecundarioUrl())
                .mascota1Path(cmd.mascota1Url())
                .mascota2Path(cmd.mascota2Url())
                .telefono(cmd.telefono())
                .telefonoSecundario(cmd.telefonoSecundario())
                .whatsapp(cmd.whatsapp())
                .correo(cmd.correo())
                .correoSecundario(cmd.correoSecundario())
                .facebookUrl(cmd.facebookUrl())
                .instagramUrl(cmd.instagramUrl())
                .tiktokUrl(cmd.tiktokUrl())
                .youtubeUrl(cmd.youtubeUrl())
                .copyrightTexto(cmd.copyrightTexto())
                .metricasNegocio(cmd.metricasNegocio())
                .esMantenimientoActivo(cmd.mantenimientoActivo())
                .mensajeMantenimiento(cmd.mensajeMantenimiento())
                .updatedBy(supabaseAuthFacade.usuarioActualId().orElse(null))
                .build();

        ConfiguracionPublicaQuery resultado = ConfiguracionPublicaQuery.from(configRepository.save(actualizado));

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                supabaseAuthFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_CMS,
                "ConfiguracionPublica", null,
                null, cmd.nombreNegocio(),
                "Configuración pública actualizada",
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        return resultado;
    }

    private ConfiguracionPublica obtenerOAutocrear() {
        return configRepository.findFirst()
                .orElseGet(() -> configRepository.save(ConfiguracionPublica.builder()
                        .nombreNegocio("Mi Negocio")
                        .slogan("Slogan por defecto")
                        .esMantenimientoActivo(false)
                        .mensajeMantenimiento("Estamos en mantenimiento, por favor regrese más tarde.")
                        .build()));
    }
}
