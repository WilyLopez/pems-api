package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorCambioCorreoAlerta implements RenderizadorCorreoTransaccional {

    private final ClientePerfilRepository clientePerfilRepository;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "CAMBIO_CORREO_ALERTA";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        ClientePerfil cliente = clientePerfilRepository.buscarPorId(notificacion.getDestinatarioClienteId())
                .orElseThrow(() -> new ResourceNotFoundException("ClientePerfil", notificacion.getDestinatarioClienteId()));

        Map<String, String> variables = Map.of(
                "titulo", notificacion.getTitulo() != null ? notificacion.getTitulo() : "",
                "mensaje", notificacion.getMensaje() != null ? notificacion.getMensaje() : "");

        return ContenidoCorreo.builder()
                .destinatario(cliente.getCorreo())
                .asunto("Solicitud de cambio de correo — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-aviso-seguridad", variables))
                .build();
    }
}
