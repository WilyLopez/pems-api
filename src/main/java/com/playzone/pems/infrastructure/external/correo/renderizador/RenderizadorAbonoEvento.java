package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class RenderizadorAbonoEvento implements RenderizadorCorreoTransaccional {

    private final EventoPrivadoRepository eventoRepository;
    private final ClientePerfilRepository clientePerfilRepository;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public String tipoCodigo() {
        return "EVENTO_ABONO_RECIBIDO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        EventoPrivado evento = eventoRepository.findById(notificacion.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", notificacion.getEntidadId()));
        ClientePerfil cliente = clientePerfilRepository.buscarPorId(evento.getIdCliente())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente", evento.getIdCliente()));

        BigDecimal saldoRestante = evento.calcularMontoSaldo() != null ? evento.calcularMontoSaldo() : BigDecimal.ZERO;

        Map<String, String> variables = Map.of(
                "nombreCliente", cliente.nombreCompleto() != null ? cliente.nombreCompleto() : "",
                "fechaEvento", evento.getFechaEvento().toString(),
                "montoAbonado", extraerMontoAbonado(notificacion.getMetadata()),
                "saldoRestante", saldoRestante.toString());

        String cuerpoHtml = templateService.procesarTemplate("email-evento-abono", variables);

        return ContenidoCorreo.builder()
                .destinatario(cliente.getCorreo())
                .asunto("Abono recibido — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }

    private String extraerMontoAbonado(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return "0.00";
        }
        try {
            JsonNode nodo = objectMapper.readTree(metadataJson);
            String monto = nodo.path("montoAbonado").asText(null);
            return monto != null && !monto.isBlank() ? monto : "0.00";
        } catch (Exception e) {
            return "0.00";
        }
    }
}
