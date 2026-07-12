package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
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
public class RenderizadorEventoCancelado implements RenderizadorCorreoTransaccional {

    private final EventoPrivadoRepository eventoRepository;
    private final ClientePerfilRepository clientePerfilRepository;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "EVENTO_CANCELADO_ADMIN";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        EventoPrivado evento = eventoRepository.findById(notificacion.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", notificacion.getEntidadId()));
        ClientePerfil cliente = clientePerfilRepository.buscarPorId(evento.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", evento.getIdCliente()));

        Map<String, String> variables = Map.of(
                "nombreCliente", cliente.nombreCompleto() != null ? cliente.nombreCompleto() : "",
                "fechaEvento", evento.getFechaEvento().toString(),
                "motivo", evento.getMotivoCancelacion() != null ? evento.getMotivoCancelacion() : "");

        String cuerpoHtml = templateService.procesarTemplate("email-evento-cancelado", variables);

        return ContenidoCorreo.builder()
                .destinatario(cliente.getCorreo())
                .asunto("Evento privado cancelado — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }
}
