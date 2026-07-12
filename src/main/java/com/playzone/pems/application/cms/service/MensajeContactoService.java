package com.playzone.pems.application.cms.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.cms.port.in.GestionarMensajeContactoUseCase;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.cms.model.MensajeContacto;
import com.playzone.pems.domain.cms.repository.MensajeContactoRepository;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.external.correo.JavaMailCorreoClient;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.HtmlEscapeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MensajeContactoService implements GestionarMensajeContactoUseCase {

    private final MensajeContactoRepository   repository;
    private final JavaMailCorreoClient        correoClient;
    private final ResolverAdministradoresPort resolverAdministradoresPort;
    private final PerfilUsuarioRepository     perfilUsuarioRepository;
    private final SupabaseAuthFacade          authFacade;
    private final RegistrarLogUseCase         auditoria;

    @Override
    @Transactional
    public MensajeContacto registrar(RegistrarCommand cmd) {
        if (cmd.getNombre() == null || cmd.getNombre().isBlank()) {
            throw new ValidationException("nombre", "El nombre es obligatorio.");
        }
        if (cmd.getCorreo() == null || cmd.getCorreo().isBlank()) {
            throw new ValidationException("correo", "El correo es obligatorio.");
        }
        if (cmd.getMensaje() == null || cmd.getMensaje().isBlank()) {
            throw new ValidationException("mensaje", "El mensaje es obligatorio.");
        }

        MensajeContacto mensaje = MensajeContacto.builder()
                .nombre(cmd.getNombre())
                .correo(cmd.getCorreo())
                .telefono(cmd.getTelefono())
                .asunto(cmd.getAsunto())
                .mensaje(cmd.getMensaje())
                .estado("PENDIENTE")
                .ipOrigen(cmd.getIpOrigen())
                .userAgent(cmd.getUserAgent())
                .build();

        MensajeContacto guardado = repository.save(mensaje);

        try {
            List<String> correosAdmin = resolverCorreosAdministradores();
            if (correosAdmin.isEmpty()) {
                log.warn("no hay administradores activos, omitiendo alerta de contacto");
            } else {
                String asuntoEmail = "[Contacto Web] Nuevo mensaje de " + cmd.getNombre();
                String cuerpoHtml = String.format(
                        "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;border:1px solid #e2e8f0;padding:20px;border-radius:12px;'>" +
                        "<h2 style='color:#00AEEF;margin-top:0;'>Nuevo mensaje de contacto recibido</h2>" +
                        "<p><b>Nombre:</b> %s</p>" +
                        "<p><b>Correo:</b> %s</p>" +
                        "<p><b>Teléfono:</b> %s</p>" +
                        "<p><b>Asunto:</b> %s</p>" +
                        "<div style='background:#f8fafc;padding:12px;border-radius:8px;border:1px solid #f1f5f9;margin-top:16px;'>" +
                        "<p style='margin:0;font-size:14px;white-space:pre-wrap;'>%s</p>" +
                        "</div>" +
                        "<p style='font-size:11px;color:#94a3b8;margin-top:20px;'>IP Origen: %s</p>" +
                        "</div>",
                        HtmlEscapeUtil.escapar(cmd.getNombre()), HtmlEscapeUtil.escapar(cmd.getCorreo()),
                        cmd.getTelefono() != null ? HtmlEscapeUtil.escapar(cmd.getTelefono()) : "—",
                        cmd.getAsunto() != null ? HtmlEscapeUtil.escapar(cmd.getAsunto()) : "—",
                        HtmlEscapeUtil.escapar(cmd.getMensaje()), HtmlEscapeUtil.escapar(cmd.getIpOrigen())
                );
                for (String correoAdmin : correosAdmin) {
                    correoClient.enviar(correoAdmin, asuntoEmail, cuerpoHtml);
                }
            }
        } catch (Exception e) {
            log.error("No se pudo enviar correo de alerta de contacto a admin: {}", e.getMessage());
        }

        return guardado;
    }

    @Override
    @Transactional
    public MensajeContacto responder(ResponderCommand cmd) {
        MensajeContacto mensaje = findOrThrow(cmd.getIdMensaje());
        if (cmd.getRespuesta() == null || cmd.getRespuesta().isBlank()) {
            throw new ValidationException("respuesta", "La respuesta no puede estar vacía.");
        }

        MensajeContacto respondido = mensaje.toBuilder()
                .estado("RESPONDIDO")
                .respuesta(cmd.getRespuesta())
                .respondidoPor(cmd.getIdUsuarioAdmin())
                .respondidoAt(OffsetDateTime.now())
                .build();

        MensajeContacto guardado = repository.save(respondido);

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                cmd.getIdUsuarioAdmin(),
                AuditoriaConstants.ACCION_RESPONDER, AuditoriaConstants.MOD_MENSAJES,
                "MensajeContacto", cmd.getIdMensaje(),
                "PENDIENTE", "RESPONDIDO",
                "Mensaje respondido al cliente: " + mensaje.getCorreo(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));

        // Enviar correo al cliente con la respuesta
        try {
            String asuntoEmail = "Respuesta a tu consulta — PlayZone";
            String cuerpoHtml = String.format(
                    "<div style='font-family:Arial,sans-serif;max-width:550px;margin:0 auto;border:1px solid #e2e8f0;padding:24px;border-radius:12px;'>" +
                    "<h3 style='color:#F64B8A;margin-top:0;'>Hola %s,</h3>" +
                    "<p style='color:#475569;'>Gracias por escribirnos. Con respecto a tu consulta:</p>" +
                    "<div style='background:#f1f5f9;padding:14px;border-radius:8px;font-style:italic;color:#1e293b;margin:12px 0;'>" +
                    "\"%s\"" +
                    "</div>" +
                    "<p style='color:#475569;'>Te respondemos lo siguiente:</p>" +
                    "<div style='background:#ecfdf5;border-left:4px solid #10b981;padding:14px;border-radius:4px;color:#065f46;margin:12px 0;font-weight:500;white-space:pre-wrap;'>" +
                    "%s" +
                    "</div>" +
                    "<p style='color:#475569;margin-top:20px;'>Atentamente,<br><b>El equipo de PlayZone</b></p>" +
                    "<hr style='border:0;border-top:1px solid #e2e8f0;margin-top:24px;'>" +
                    "<p style='font-size:11px;color:#94a3b8;'>Por favor, no respondas a este correo. Fue generado automáticamente.</p>" +
                    "</div>",
                    HtmlEscapeUtil.escapar(mensaje.getNombre()), HtmlEscapeUtil.escapar(mensaje.getMensaje()),
                    HtmlEscapeUtil.escapar(cmd.getRespuesta())
            );
            correoClient.enviar(mensaje.getCorreo(), asuntoEmail, cuerpoHtml);
        } catch (Exception e) {
            log.error("No se pudo enviar correo de respuesta al cliente {}: {}", mensaje.getCorreo(), e.getMessage());
        }

        return guardado;
    }

    @Override
    @Transactional
    public MensajeContacto marcarComoLeido(Long idMensaje) {
        MensajeContacto mensaje = findOrThrow(idMensaje);
        return repository.save(mensaje.toBuilder().estado("LEIDO").build());
    }

    @Override
    @Transactional
    public MensajeContacto marcarComoSpam(Long idMensaje) {
        MensajeContacto mensaje = findOrThrow(idMensaje);
        MensajeContacto guardado = repository.save(mensaje.toBuilder().estado("SPAM").build());
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_MARCAR_SPAM, AuditoriaConstants.MOD_MENSAJES,
                "MensajeContacto", idMensaje,
                mensaje.getEstado(), "SPAM",
                "Mensaje marcado como SPAM: " + mensaje.getCorreo(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));
        return guardado;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MensajeContacto> listar(String estado, Pageable pageable) {
        if (estado != null && !estado.isBlank()) {
            return repository.findByEstado(estado, pageable);
        }
        return repository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public MensajeContacto obtener(Long idMensaje) {
        return findOrThrow(idMensaje);
    }

    @Override
    @Transactional
    public void eliminar(Long idMensaje) {
        findOrThrow(idMensaje);
        repository.deleteById(idMensaje);
    }

    private MensajeContacto findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MensajeContacto", id));
    }

    private List<String> resolverCorreosAdministradores() {
        return resolverAdministradoresPort.obtenerIdsAdministradoresActivos().stream()
                .map(perfilUsuarioRepository::buscarPorId)
                .flatMap(Optional::stream)
                .map(PerfilUsuario::getCorreo)
                .filter(correo -> correo != null && !correo.isBlank())
                .toList();
    }
}
