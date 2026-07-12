package com.playzone.pems.infrastructure.external.correo;

import com.playzone.pems.infrastructure.external.correo.renderizador.AdjuntoCorreo;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailCorreoClient {

    private final JavaMailSender mailSender;

    @Value("${playzone.correo.remitente}")
    private String remitente;

    @Value("${playzone.correo.nombre-remitente:PlayZone}")
    private String nombreRemitente;

    public void enviar(String destinatario, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            mailSender.send(mensaje);
            log.info("Correo enviado a {}: {}", destinatario, asunto);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo electrónico.", e);
        }
    }

    public void enviarConLogo(String destinatario, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            helper.addInline("logo", new ClassPathResource("static/logo.png"));
            mailSender.send(mensaje);
            log.info("Correo enviado a {}: {}", destinatario, asunto);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error al enviar correo a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo electrónico.", e);
        }
    }

    public void enviarConAdjuntos(String destinatario, String asunto, String cuerpoHtml, List<AdjuntoCorreo> adjuntos) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
            helper.setFrom(remitente, nombreRemitente);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true);
            for (AdjuntoCorreo adjunto : adjuntos) {
                helper.addAttachment(
                        adjunto.getNombreArchivo(),
                        new ByteArrayResource(adjunto.getContenido()),
                        adjunto.getTipoContenido());
            }
            mailSender.send(mensaje);
            log.info("Correo con {} adjunto(s) enviado a {}: {}", adjuntos.size(), destinatario, asunto);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Error al enviar correo con adjuntos a {}: {}", destinatario, e.getMessage(), e);
            throw new RuntimeException("Error al enviar correo electrónico.", e);
        }
    }
}