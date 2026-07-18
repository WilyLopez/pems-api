package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.model.StaffPerfil;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.domain.usuario.repository.UsuarioRolRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RenderizadorBienvenidaStaff implements RenderizadorCorreoTransaccional {

    private final StaffPerfilRepository staffPerfilRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final SedeRepository sedeRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final TemplateService templateService;
    private final ObjectMapper objectMapper;

    @Value("${playzone.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public String tipoCodigo() {
        return "USUARIO_ACTIVACION";
    }

    @Override
    public ContenidoCorreo renderizar(Notificacion notificacion) {
        StaffPerfil staff = staffPerfilRepository.buscarPorId(notificacion.getEntidadId())
                .orElseThrow(() -> new ResourceNotFoundException("StaffPerfil", notificacion.getEntidadId()));
        PerfilUsuario perfil = perfilUsuarioRepository.buscarPorId(staff.getUsuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("PerfilUsuario", "usuarioId", staff.getUsuarioId()));
        Sede sede = sedeRepository.findById(staff.getSedeId()).orElse(null);
        List<String> roles = usuarioRolRepository.listarCodigosRolPorUsuario(staff.getUsuarioId());
        String rolLabel = roles.contains("ADMIN") ? "Administrador"
                : roles.contains("CAJERO") ? "Cajero"
                : roles.isEmpty() ? "" : roles.get(0);

        Map<String, String> variables = Map.of(
                "nombre", perfil.getNombreCompleto() != null ? perfil.getNombreCompleto() : "",
                "correo", perfil.getCorreo() != null ? perfil.getCorreo() : "",
                "rol", rolLabel,
                "sede", sede != null ? sede.getNombre() : "Sede Principal",
                "activationUrl", frontendUrl + "/auth/activar-cuenta?token=" + extraerToken(notificacion.getMetadata()));

        String cuerpoHtml = templateService.procesarTemplate("welcome-user", variables);

        return ContenidoCorreo.builder()
                .destinatario(perfil.getCorreo())
                .asunto("Activa tu cuenta — Kiki y Lala")
                .cuerpoHtml(cuerpoHtml)
                .build();
    }

    private String extraerToken(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            throw new IllegalStateException("La notificación de activación no contiene el token requerido.");
        }
        try {
            JsonNode nodo = objectMapper.readTree(metadataJson);
            String token = nodo.path("tokenActivacion").asText(null);
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("La notificación de activación no contiene el token requerido.");
            }
            return token;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo leer el token de activación.", e);
        }
    }
}
