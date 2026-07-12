package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorEventoConfirmado implements RenderizadorCorreoTransaccional {

    private final EventoPrivadoRepository eventoRepository;
    private final ClientePerfilRepository clientePerfilRepository;
    private final TurnoRepository turnoRepository;
    private final TemplateService templateService;

    @Override
    public String tipoCodigo() {
        return "EVENTO_CONFIRMADO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        EventoPrivado evento = eventoRepository.findById(notificacion.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", notificacion.getEntidadId()));
        ClientePerfil cliente = clientePerfilRepository.buscarPorId(evento.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", evento.getIdCliente()));
        Turno turno = turnoRepository.findById(evento.getIdTurno())
                .orElseThrow(() -> new ResourceNotFoundException("Turno", evento.getIdTurno()));

        Map<String, String> variables = Map.of(
                "nombreCliente", cliente.nombreCompleto() != null ? cliente.nombreCompleto() : "",
                "tipoEvento", evento.getNombreTipoEvento() != null ? evento.getNombreTipoEvento() : evento.getTipoEvento(),
                "fechaEvento", evento.getFechaEvento().toString(),
                "turno", turno.getDescripcion(),
                "horaInicio", turno.getHoraInicio().toString(),
                "horaFin", turno.getHoraFin().toString(),
                "aforoDeclarado", evento.getAforoDeclarado() != null ? evento.getAforoDeclarado().toString() : "-",
                "precioTotalContrato", String.valueOf(evento.getPrecioContrato()),
                "montoAdelanto", String.valueOf(evento.getMontoAdelanto()),
                "montoSaldo", String.valueOf(evento.calcularMontoSaldo() != null ? evento.calcularMontoSaldo() : BigDecimal.ZERO));

        String cuerpoHtml = templateService.procesarTemplate("email-evento-confirmado", variables);

        return ContenidoCorreo.builder()
                .destinatario(cliente.getCorreo())
                .asunto("¡Tu evento privado ha sido confirmado! — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }
}
