package com.playzone.pems.infrastructure.external.correo;

import com.playzone.pems.application.cms.port.in.GestionarConfiguracionPublicaUseCase;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.port.out.EnviarNotificacionEventoPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;
import com.playzone.pems.application.venta.dto.query.VentaQuery;
import com.playzone.pems.application.venta.port.out.EnviarDocumentosVentaPort;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.external.correo.renderizador.AdjuntoCorreo;
import com.playzone.pems.infrastructure.pdf.NotaVentaPdfService;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.util.HtmlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoAdapter
        implements EnviarNotificacionEventoPort,
        EnviarDocumentosVentaPort {

    private final JavaMailCorreoClient            correoClient;
    private final TicketIngresoPdfService         ticketIngresoPdfService;
    private final NotaVentaPdfService             notaVentaPdfService;
    private final SedeRepository                  sedeRepository;
    private final TemplateService                 templateService;
    private final GestionarConfiguracionPublicaUseCase configuracionPublica;
    private final ResolverAdministradoresPort     resolverAdministradoresPort;
    private final PerfilUsuarioRepository         perfilUsuarioRepository;

    @Async("asyncExecutor")
    @Override
    public void notificarAdminNuevaSolicitud(EventoPrivadoQuery evento) {
        List<String> correosAdmin = resolverCorreosAdministradores();
        if (correosAdmin.isEmpty()) {
            log.warn("no hay administradores activos, omitiendo notificacion de nueva solicitud");
            return;
        }
        String asunto = "[Nueva solicitud] " + evento.getTipoEvento() + " — " + evento.getFechaEvento();
        String cuerpo = htmlBase(
            "Nueva solicitud de evento",
            "<p>Se ha recibido una nueva solicitud de evento privado:</p>"
            + resumenEvento(evento)
            + "<p><b>Cliente:</b> " + HtmlEscapeUtil.escapar(evento.getNombreCliente()) + "</p>"
            + "<p><b>Correo:</b> " + HtmlEscapeUtil.escapar(evento.getCorreoCliente()) + "</p>"
            + "<p><b>Teléfono:</b> " + (evento.getTelefonoCliente() != null ? HtmlEscapeUtil.escapar(evento.getTelefonoCliente()) : "—") + "</p>"
            + "<p style='margin-top:16px;'><a href='#' style='background:#00AEEF;color:white;padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:bold;'>Ver en el panel</a></p>"
        );
        for (String correoAdmin : correosAdmin) {
            correoClient.enviar(correoAdmin, asunto, cuerpo);
        }
    }

    private List<String> resolverCorreosAdministradores() {
        return resolverAdministradoresPort.obtenerIdsAdministradoresActivos().stream()
                .map(perfilUsuarioRepository::buscarPorId)
                .flatMap(Optional::stream)
                .map(PerfilUsuario::getCorreo)
                .filter(correo -> correo != null && !correo.isBlank())
                .toList();
    }

    private String htmlBase(String titulo, String contenido) {
        return "<div style='font-family:Arial,sans-serif;max-width:520px;margin:0 auto;'>"
            + "<div style='background:#F64B8A;padding:20px;border-radius:12px 12px 0 0;text-align:center;'>"
            + "<h2 style='color:white;margin:0;font-size:18px;'>" + titulo + "</h2>"
            + "</div>"
            + "<div style='background:#f8fafc;padding:24px;border:1px solid #e2e8f0;border-radius:0 0 12px 12px;'>"
            + contenido
            + "</div></div>";
    }

    private String resumenEvento(EventoPrivadoQuery evento) {
        return "<div style='background:white;border:1px solid #e2e8f0;border-radius:8px;padding:16px;margin:16px 0;font-size:13px;'>"
            + filaEvento("Tipo de evento", evento.getTipoEvento())
            + filaEvento("Fecha", evento.getFechaEvento() != null ? evento.getFechaEvento().toString() : "—")
            + filaEvento("Turno", evento.getTurno() + " · " + evento.getHoraInicio() + " – " + evento.getHoraFin())
            + (evento.getAforoDeclarado() != null ? filaEvento("Invitados", evento.getAforoDeclarado() + " personas") : "")
            + "</div>";
    }

    private String filaEvento(String label, String valor) {
        return "<p style='margin:6px 0;'><span style='color:#64748b;'>" + label + ":</span> <b>" + valor + "</b></p>";
    }

    @Override
    public void enviarDocumentos(String destinatario, VentaDetalleQuery ventaDetalle) {
        String nombreSede = sedeRepository.findById(ventaDetalle.getIdSede())
                .map(s -> s.getNombre())
                .orElse("Sede Principal");

        VentaQuery ventaQuery = VentaQuery.builder()
                .id(ventaDetalle.getId())
                .idSede(ventaDetalle.getIdSede())
                .clienteId(ventaDetalle.getClienteId())
                .eventoId(ventaDetalle.getEventoId())
                .tipo(ventaDetalle.getTipo())
                .canalCodigo(ventaDetalle.getCanalCodigo())
                .fechaVisita(ventaDetalle.getFechaVisita())
                .subtotal(ventaDetalle.getSubtotal())
                .descuento(ventaDetalle.getDescuento())
                .total(ventaDetalle.getTotal())
                .nombreAcompanante(ventaDetalle.getNombreAcompanante())
                .dniAcompanante(ventaDetalle.getDniAcompanante())
                .nombreCliente(ventaDetalle.getNombreCliente())
                .notas(ventaDetalle.getNotas())
                .impreso(ventaDetalle.isImpreso())
                .enviadoCorreo(ventaDetalle.isEnviadoCorreo())
                .descargado(ventaDetalle.isDescargado())
                .efectivoRecibido(ventaDetalle.getEfectivoRecibido())
                .vuelto(ventaDetalle.getVuelto())
                .createdAt(ventaDetalle.getCreatedAt())
                .build();

        byte[] pdfNota = notaVentaPdfService.generarNotaVentaPdf(ventaQuery, nombreSede);

        try {
            String nombreRemitente = configuracionPublica.obtener().getNombreNegocio();
            if (nombreRemitente == null || nombreRemitente.isBlank()) {
                nombreRemitente = "PlayZone";
            }

            Map<String, String> variables = Map.of(
                    "nombreCliente", ventaDetalle.getNombreCliente() != null ? ventaDetalle.getNombreCliente() : "Cliente",
                    "ventaId",       ventaDetalle.getId().toString(),
                    "fechaVisita",   ventaDetalle.getFechaVisita().toString(),
                    "total",         ventaDetalle.getTotal().toString()
            );
            String cuerpoHtml = templateService.procesarTemplate("email-venta", variables);

            List<AdjuntoCorreo> adjuntos = new ArrayList<>();
            adjuntos.add(AdjuntoCorreo.builder()
                    .nombreArchivo("NotaVenta-" + ventaDetalle.getId() + ".pdf")
                    .contenido(pdfNota)
                    .tipoContenido("application/pdf")
                    .build());

            for (var t : ventaDetalle.getTickets()) {
                byte[] pdfTicket = ticketIngresoPdfService.generarTicketPdf(t, nombreSede);
                adjuntos.add(AdjuntoCorreo.builder()
                        .nombreArchivo("Ticket-" + t.getNumeroTicket() + ".pdf")
                        .contenido(pdfTicket)
                        .tipoContenido("application/pdf")
                        .build());
            }

            correoClient.enviarConAdjuntos(
                    destinatario,
                    "Tus comprobantes Kiki y Lala — Venta #" + ventaDetalle.getId(),
                    cuerpoHtml,
                    adjuntos,
                    nombreRemitente);
            log.info("Documentos de venta consolidada enviados por correo a {}: Venta #{}", destinatario, ventaDetalle.getId());
        } catch (Exception e) {
            log.error("Error al enviar documentos de venta consolidada a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar los documentos por correo.", e);
        }
    }
}
