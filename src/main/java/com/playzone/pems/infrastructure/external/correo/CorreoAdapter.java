package com.playzone.pems.infrastructure.external.correo;

import com.playzone.pems.application.cms.port.in.GestionarConfiguracionPublicaUseCase;
import com.playzone.pems.application.evento.dto.query.EventoPrivadoQuery;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.out.EnviarNotificacionEventoPort;
import com.playzone.pems.application.evento.port.out.EnviarTicketPorCorreoPort;
import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;
import com.playzone.pems.application.venta.dto.query.VentaQuery;
import com.playzone.pems.application.venta.port.out.EnviarDocumentosVentaPort;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.NotaVentaPdfService;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import com.playzone.pems.application.usuario.port.out.EnviarCorreoBienvenidaPort;
import com.playzone.pems.infrastructure.template.TemplateService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CorreoAdapter
        implements EnviarTicketPorCorreoPort,
        EnviarNotificacionEventoPort,
        EnviarCorreoBienvenidaPort,
        EnviarDocumentosVentaPort {

    private final JavaMailCorreoClient            correoClient;
    private final JavaMailSender                  mailSender;
    private final TicketIngresoPdfService         ticketIngresoPdfService;
    private final NotaVentaPdfService             notaVentaPdfService;
    private final SedeRepository                  sedeRepository;
    private final TemplateService                 templateService;
    private final GestionarConfiguracionPublicaUseCase configuracionPublica;

    @Value("${playzone.correo.remitente}")
    private String remitente;

    @Value("${playzone.correo.alertas-admin:}")
    private String adminEmailOverride;

    @Value("${playzone.url-login:http://localhost:3000/auth/login}")
    private String loginUrl;

    @Async("asyncExecutor")
    @Override
    public void enviarCredencialesUsuario(String correo, String nombre, String password,
                                          String rolLabel, String sedeNombre) {
        String asunto = "Bienvenido al Panel Administrativo — Kiki y Lala";

        Map<String, String> variables = Map.of(
                "nombre",    nombre != null ? nombre : "",
                "correo",    correo != null ? correo : "",
                "password",  password != null ? password : "",
                "rol",       rolLabel != null ? rolLabel : "",
                "sede",      sedeNombre != null ? sedeNombre : "Sede Principal",
                "loginUrl",  loginUrl != null ? loginUrl : ""
        );

        String cuerpoHtml = templateService.procesarTemplate("welcome-user", variables);
        correoClient.enviarConLogo(correo, asunto, cuerpoHtml);
    }

    @Async("asyncExecutor")
    @Override
    public void enviarTicket(String destinatario, String nombreCliente, ReservaPublicaQuery reserva) {
        String nombreSede = sedeRepository.findById(reserva.getIdSede())
                .map(s -> s.getNombre())
                .orElse("Sede Principal");

        byte[] pdfTicket = ticketIngresoPdfService.generarTicketPdf(reserva, nombreSede);

        try {
            String nombreRemitente = configuracionPublica.obtener().getNombreNegocio();
            if (nombreRemitente == null || nombreRemitente.isBlank()) {
                nombreRemitente = "PlayZone";
            }

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject("Tu ticket Kiki y Lala — " + reserva.getNumeroTicket());

            Map<String, String> variables = Map.of(
                    "nombreCliente", nombreCliente,
                    "numeroTicket",  reserva.getNumeroTicket(),
                    "fecha",         reserva.getFechaEvento().toString(),
                    "total",         reserva.getTotalPagado().toString()
            );
            String cuerpoHtml = templateService.procesarTemplate("email-ticket", variables);

            helper.setText(cuerpoHtml, true);
            helper.addAttachment(
                "Ticket-" + reserva.getNumeroTicket() + ".pdf",
                new ByteArrayResource(pdfTicket),
                "application/pdf"
            );

            mailSender.send(mensaje);
            log.info("Ticket PDF enviado a {}: {}", destinatario, reserva.getNumeroTicket());
        } catch (Exception e) {
            log.error("Error al enviar ticket a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar el ticket por correo.", e);
        }
    }

    @Async("asyncExecutor")
    @Override
    public void notificarSolicitudRecibida(String destinatario, EventoPrivadoQuery evento) {
        String asunto = "Solicitud de evento privado recibida — Kiki y Lala";
        String cuerpo = htmlBase(
            "Solicitud recibida",
            "<p>Hola <b>" + evento.getNombreCliente() + "</b>,</p>"
            + "<p>Hemos recibido tu solicitud de evento privado. Nuestro equipo se pondrá en contacto contigo en menos de 24 horas.</p>"
            + resumenEvento(evento)
            + "<p style='color:#64748b;font-size:13px;margin-top:20px;'>Próximos pasos: te confirmaremos disponibilidad, precio y condiciones por este medio.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void notificarEventoConfirmado(String destinatario, EventoPrivadoQuery evento) {
        String asunto = "Tu evento privado ha sido confirmado — Kiki y Lala";
        String cuerpo = htmlBase(
            "¡Evento confirmado!",
            "<p>Hola <b>" + evento.getNombreCliente() + "</b>,</p>"
            + "<p>Tu evento ha sido confirmado. Te compartimos el resumen:</p>"
            + resumenEvento(evento)
            + filaFinanciera("Total contratado", "S/ " + evento.getPrecioTotalContrato())
            + filaFinanciera("Adelanto pagado", "S/ " + evento.getMontoAdelanto())
            + filaFinanciera("Saldo pendiente", "S/ " + evento.getMontoSaldo())
            + "<p style='color:#64748b;font-size:13px;margin-top:16px;'>El contrato será enviado próximamente. El saldo pendiente se abona el día del evento.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void notificarEventoCancelado(String destinatario, EventoPrivadoQuery evento, String motivo) {
        String asunto = "Evento privado cancelado — Kiki y Lala";
        String cuerpo = htmlBase(
            "Evento cancelado",
            "<p>Hola <b>" + evento.getNombreCliente() + "</b>,</p>"
            + "<p>Lamentamos informarte que el evento del <b>" + evento.getFechaEvento() + "</b> ha sido cancelado.</p>"
            + "<p><b>Motivo:</b> " + motivo + "</p>"
            + "<p style='color:#64748b;font-size:13px;'>Si tienes dudas, contáctanos por WhatsApp o correo.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void notificarAbonoRecibido(String destinatario, EventoPrivadoQuery evento,
                                       java.math.BigDecimal montoAbonado, java.math.BigDecimal saldoRestante) {
        String asunto = "Abono recibido — Kiki y Lala";
        String cuerpo = htmlBase(
            "Abono recibido",
            "<p>Hola <b>" + evento.getNombreCliente() + "</b>,</p>"
            + "<p>Hemos registrado tu abono para el evento del <b>" + evento.getFechaEvento() + "</b>.</p>"
            + filaFinanciera("Monto abonado", "S/ " + montoAbonado)
            + filaFinanciera("Saldo pendiente", "S/ " + saldoRestante)
            + "<p style='color:#64748b;font-size:13px;margin-top:16px;'>Gracias por tu pago.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void notificarAdminNuevaSolicitud(EventoPrivadoQuery evento) {
        String correoAdmin = adminEmailOverride;
        if (correoAdmin == null || correoAdmin.isBlank()) {
            correoAdmin = configuracionPublica.obtener().getCorreo();
        }
        if (correoAdmin == null || correoAdmin.isBlank()) {
            log.warn("correo admin no configurado, omitiendo notificacion de nueva solicitud");
            return;
        }
        String asunto = "[Nueva solicitud] " + evento.getTipoEvento() + " — " + evento.getFechaEvento();
        String cuerpo = htmlBase(
            "Nueva solicitud de evento",
            "<p>Se ha recibido una nueva solicitud de evento privado:</p>"
            + resumenEvento(evento)
            + "<p><b>Cliente:</b> " + evento.getNombreCliente() + "</p>"
            + "<p><b>Correo:</b> " + evento.getCorreoCliente() + "</p>"
            + "<p><b>Teléfono:</b> " + (evento.getTelefonoCliente() != null ? evento.getTelefonoCliente() : "—") + "</p>"
            + "<p style='margin-top:16px;'><a href='#' style='background:#00AEEF;color:white;padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:bold;'>Ver en el panel</a></p>"
        );
        correoClient.enviar(correoAdmin, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void enviarReservaPendiente(String destinatario, String nombreCliente, ReservaPublicaQuery reserva) {
        String asunto = "Reserva pendiente de confirmacion — Kiki y Lala";
        String cuerpo = htmlBase(
            "Reserva pendiente",
            "<p>Hola <b>" + nombreCliente + "</b>,</p>"
            + "<p>Tu reserva <b>#" + reserva.getNumeroTicket() + "</b> para el dia <b>" + reserva.getFechaEvento() + "</b> ha sido recibida y esta pendiente de confirmacion.</p>"
            + "<p style='color:#64748b;font-size:13px;'>Nuestro equipo revisara tu solicitud y te notificaremos a la brevedad.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void enviarReservaRechazada(String destinatario, String nombreCliente, ReservaPublicaQuery reserva, String motivo) {
        String asunto = "Reserva rechazada — Kiki y Lala";
        String cuerpo = htmlBase(
            "Reserva rechazada",
            "<p>Hola <b>" + nombreCliente + "</b>,</p>"
            + "<p>Tu reserva para el dia <b>" + reserva.getFechaEvento() + "</b> no pudo ser aprobada.</p>"
            + (motivo != null ? "<p><b>Motivo:</b> " + motivo + "</p>" : "")
            + "<p style='color:#64748b;font-size:13px;'>Si tienes dudas, contactanos por WhatsApp o correo.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
    }

    @Async("asyncExecutor")
    @Override
    public void enviarReservaCancelada(String destinatario, String nombreCliente, ReservaPublicaQuery reserva, String motivo) {
        String asunto = "Reserva cancelada — Kiki y Lala";
        String cuerpo = htmlBase(
            "Reserva cancelada",
            "<p>Hola <b>" + nombreCliente + "</b>,</p>"
            + "<p>Tu reserva <b>#" + reserva.getNumeroTicket() + "</b> para el dia <b>" + reserva.getFechaEvento() + "</b> ha sido cancelada.</p>"
            + (motivo != null ? "<p><b>Motivo:</b> " + motivo + "</p>" : "")
            + "<p style='color:#64748b;font-size:13px;'>Si deseas reagendar, escribenos por WhatsApp.</p>"
        );
        correoClient.enviarConLogo(destinatario, asunto, cuerpo);
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

    private String filaFinanciera(String label, String valor) {
        return "<p style='margin:6px 0;display:flex;justify-content:space-between;'>"
            + "<span style='color:#64748b;'>" + label + "</span>"
            + "<b style='color:#1A1A2E;'>" + valor + "</b></p>";
    }

    @Async("asyncExecutor")
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

            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject("Tus comprobantes Kiki y Lala — Venta #" + ventaDetalle.getId());

            Map<String, String> variables = Map.of(
                    "nombreCliente", ventaDetalle.getNombreCliente() != null ? ventaDetalle.getNombreCliente() : "Cliente",
                    "ventaId",       ventaDetalle.getId().toString(),
                    "fechaVisita",   ventaDetalle.getFechaVisita().toString(),
                    "total",         ventaDetalle.getTotal().toString()
            );
            String cuerpoHtml = templateService.procesarTemplate("email-venta", variables);

            helper.setText(cuerpoHtml, true);
            helper.addAttachment(
                "NotaVenta-" + ventaDetalle.getId() + ".pdf",
                new ByteArrayResource(pdfNota),
                "application/pdf"
            );

            List<ByteArrayResource> ticketResources = new ArrayList<>();
            for (var t : ventaDetalle.getTickets()) {
                byte[] pdfTicket = ticketIngresoPdfService.generarTicketPdf(t, nombreSede);
                helper.addAttachment(
                    "Ticket-" + t.getNumeroTicket() + ".pdf",
                    new ByteArrayResource(pdfTicket),
                    "application/pdf"
                );
            }

            mailSender.send(mensaje);
            log.info("Documentos de venta consolidada enviados por correo a {}: Venta #{}", destinatario, ventaDetalle.getId());
        } catch (Exception e) {
            log.error("Error al enviar documentos de venta consolidada a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar los documentos por correo.", e);
        }
    }
}
