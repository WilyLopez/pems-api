package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorEventoRecordatorio3Dias implements RenderizadorCorreoTransaccional {

    private final EventoPrivadoRepository eventoRepository;
    private final ClientePerfilRepository clientePerfilRepository;
    private final SedeRepository sedeRepository;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "EVENTO_RECORDATORIO_3DIAS";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        EventoPrivado evento = eventoRepository.findById(notificacion.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", notificacion.getEntidadId()));
        ClientePerfil cliente = clientePerfilRepository.buscarPorId(evento.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", evento.getIdCliente()));
        String nombreSede = sedeRepository.findById(evento.getIdSede()).map(Sede::getNombre).orElse("");

        Map<String, String> variables = Map.of(
                "nombreCliente", cliente.nombreCompleto() != null ? cliente.nombreCompleto() : "",
                "sede", nombreSede,
                "fecha", evento.getFechaEvento() != null ? evento.getFechaEvento().toString() : "");

        return ContenidoCorreo.builder()
                .destinatario(cliente.getCorreo())
                .asunto("Tu evento es en 3 días — Kiki y Lala")
                .cuerpoHtml(templateService.procesarTemplate("email-evento-recordatorio", variables))
                .build();
    }
}
