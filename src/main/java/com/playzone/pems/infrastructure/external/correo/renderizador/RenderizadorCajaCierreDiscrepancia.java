package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorCajaCierreDiscrepancia implements RenderizadorCorreoTransaccional {

    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "CAJA_CIERRE_DISCREPANCIA";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        PerfilUsuario perfil = perfilUsuarioRepository.buscarPorId(notificacion.getDestinatarioUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("PerfilUsuario", "usuarioId", notificacion.getDestinatarioUsuarioId()));

        Map<String, String> variables = Map.of(
                "titulo", notificacion.getTitulo() != null ? notificacion.getTitulo() : "",
                "mensaje", notificacion.getMensaje() != null ? notificacion.getMensaje() : "");

        return ContenidoCorreo.builder()
                .destinatario(perfil.getCorreo())
                .asunto("Discrepancia en cierre de caja — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-aviso-seguridad", variables))
                .build();
    }
}
