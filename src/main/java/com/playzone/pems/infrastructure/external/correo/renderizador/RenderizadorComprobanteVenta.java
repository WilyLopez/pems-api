package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;
import com.playzone.pems.application.venta.dto.query.VentaQuery;
import com.playzone.pems.application.venta.port.in.ConsultarVentasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.NotaVentaPdfService;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import com.playzone.pems.infrastructure.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorComprobanteVenta implements RenderizadorCorreoTransaccional {

    private final ConsultarVentasUseCase consultarVentasUseCase;
    private final SedeRepository sedeRepository;
    private final NotaVentaPdfService notaVentaPdfService;
    private final TicketIngresoPdfService ticketIngresoPdfService;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public String tipoCodigo() {
        return "DOCUMENTO_LISTO";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        VentaDetalleQuery detalle = consultarVentasUseCase.consultarDetallePorId(notificacion.getEntidadId());
        String nombreSede = sedeRepository.findById(detalle.getIdSede())
                .map(sede -> sede.getNombre())
                .orElse("Sede Principal");

        VentaQuery ventaQuery = VentaQuery.builder()
                .id(detalle.getId())
                .idSede(detalle.getIdSede())
                .clienteId(detalle.getClienteId())
                .eventoId(detalle.getEventoId())
                .tipo(detalle.getTipo())
                .canalCodigo(detalle.getCanalCodigo())
                .fechaVisita(detalle.getFechaVisita())
                .subtotal(detalle.getSubtotal())
                .descuento(detalle.getDescuento())
                .total(detalle.getTotal())
                .nombreAcompanante(detalle.getNombreAcompanante())
                .dniAcompanante(detalle.getDniAcompanante())
                .nombreCliente(detalle.getNombreCliente())
                .notas(detalle.getNotas())
                .impreso(detalle.isImpreso())
                .enviadoCorreo(detalle.isEnviadoCorreo())
                .descargado(detalle.isDescargado())
                .efectivoRecibido(detalle.getEfectivoRecibido())
                .vuelto(detalle.getVuelto())
                .createdAt(detalle.getCreatedAt())
                .build();

        byte[] pdfNota = notaVentaPdfService.generarNotaVentaPdf(ventaQuery, nombreSede);

        List<AdjuntoCorreo> adjuntos = new ArrayList<>();
        adjuntos.add(AdjuntoCorreo.builder()
                .nombreArchivo("NotaVenta-" + detalle.getId() + ".pdf")
                .contenido(pdfNota)
                .tipoContenido("application/pdf")
                .build());

        for (var ticket : detalle.getTickets()) {
            byte[] pdfTicket = ticketIngresoPdfService.generarTicketPdf(ticket, nombreSede);
            adjuntos.add(AdjuntoCorreo.builder()
                    .nombreArchivo("Ticket-" + ticket.getNumeroTicket() + ".pdf")
                    .contenido(pdfTicket)
                    .tipoContenido("application/pdf")
                    .build());
        }

        Map<String, String> variables = Map.of(
                "nombreCliente", detalle.getNombreCliente() != null ? detalle.getNombreCliente() : "Cliente",
                "ventaId", detalle.getId().toString(),
                "fechaVisita", detalle.getFechaVisita() != null ? detalle.getFechaVisita().toString() : "",
                "total", detalle.getTotal().toString());

        String cuerpoHtml = templateService.procesarTemplate("email-venta", variables);

        return ContenidoCorreo.builder()
                .destinatario(extraerDestinatario(notificacion.getMetadata()))
                .asunto("Tus comprobantes Kiki y Lala — Venta #" + detalle.getId())
                .cuerpoHtml(cuerpoHtml)
                .adjuntos(adjuntos)
                .build();
    }

    private String extraerDestinatario(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return null;
        }
        try {
            JsonNode nodo = objectMapper.readTree(metadataJson);
            String destinatario = nodo.path("destinatario").asText(null);
            return destinatario != null && !destinatario.isBlank() ? destinatario : null;
        } catch (Exception e) {
            return null;
        }
    }
}
