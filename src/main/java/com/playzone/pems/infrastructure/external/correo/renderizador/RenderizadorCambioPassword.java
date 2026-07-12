package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorCambioPassword implements RenderizadorCorreoTransaccional {

    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final ClientePerfilRepository clientePerfilRepository;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "CAMBIO_PASSWORD";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        String destinatario;

        if (notificacion.getDestinatarioUsuarioId() != null) {
            PerfilUsuario perfil = perfilUsuarioRepository.buscarPorId(notificacion.getDestinatarioUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("PerfilUsuario", "usuarioId", notificacion.getDestinatarioUsuarioId()));
            destinatario = perfil.getCorreo();
        } else {
            ClientePerfil cliente = clientePerfilRepository.buscarPorId(notificacion.getDestinatarioClienteId())
                    .orElseThrow(() -> new ResourceNotFoundException("ClientePerfil", notificacion.getDestinatarioClienteId()));
            destinatario = cliente.getCorreo();
        }

        Map<String, String> variables = Map.of(
                "titulo", notificacion.getTitulo() != null ? notificacion.getTitulo() : "",
                "mensaje", notificacion.getMensaje() != null ? notificacion.getMensaje() : "");

        return ContenidoCorreo.builder()
                .destinatario(destinatario)
                .asunto("Tu contraseña fue actualizada — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-aviso-seguridad", variables))
                .build();
    }
}
