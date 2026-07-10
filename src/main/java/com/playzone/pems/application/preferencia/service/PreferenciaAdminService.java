package com.playzone.pems.application.preferencia.service;

import com.playzone.pems.application.preferencia.dto.command.ActualizarPreferenciaAdminCommand;
import com.playzone.pems.application.preferencia.dto.response.PreferenciaAdminResponse;
import com.playzone.pems.application.preferencia.port.in.ActualizarPreferenciaAdminUseCase;
import com.playzone.pems.application.preferencia.port.in.ObtenerPreferenciaAdminUseCase;
import com.playzone.pems.domain.preferencia.model.PreferenciaUsuario;
import com.playzone.pems.domain.preferencia.repository.PreferenciaUsuarioRepository;
import com.playzone.pems.shared.exception.ForbiddenException;
import com.playzone.pems.infrastructure.security.SupabaseAuthContext;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PreferenciaAdminService
        implements ObtenerPreferenciaAdminUseCase, ActualizarPreferenciaAdminUseCase {

    private static final List<String> ROLES_PERMITIDOS = List.of("ADMIN", "SUPERADMIN");

    private final PreferenciaUsuarioRepository repository;
    private final SupabaseAuthFacade           authFacade;

    @Override
    @Transactional(readOnly = true)
    public PreferenciaAdminResponse obtener(UUID usuarioId) {
        validarRol();
        PreferenciaUsuario pref = repository.buscarPorUsuarioId(usuarioId)
                .orElseGet(() -> defaults(usuarioId));
        return toResponse(pref);
    }

    @Override
    @Transactional
    public PreferenciaAdminResponse actualizar(UUID usuarioId, ActualizarPreferenciaAdminCommand cmd) {
        validarRol();
        PreferenciaUsuario pref = fromCommand(usuarioId, cmd);
        return toResponse(repository.guardar(pref));
    }

    @Override
    @Transactional
    public PreferenciaAdminResponse parchear(UUID usuarioId, Map<String, Object> patch) {
        validarRol();
        PreferenciaUsuario existing = repository.buscarPorUsuarioId(usuarioId)
                .orElseGet(() -> defaults(usuarioId));
        PreferenciaUsuario patched = applyPatch(existing, patch);
        return toResponse(repository.guardar(patched));
    }

    @Override
    @Transactional
    public PreferenciaAdminResponse resetear(UUID usuarioId) {
        validarRol();
        return toResponse(repository.guardar(defaults(usuarioId)));
    }

    private void validarRol() {
        SupabaseAuthContext ctx = authFacade.contextoActual()
                .orElseThrow(() -> new UnauthorizedException("No autenticado"));
        boolean permitido = ctx.roles().stream().anyMatch(ROLES_PERMITIDOS::contains);
        if (!permitido) {
            throw new ForbiddenException("Solo ADMIN y SUPERADMIN pueden gestionar preferencias administrativas.");
        }
    }

    private PreferenciaUsuario defaults(UUID usuarioId) {
        return PreferenciaUsuario.builder()
                .usuarioId(usuarioId)
                .tema("SYSTEM")
                .tipografia("Inter")
                .tamanioFuente("NORMAL")
                .sonidoNotificaciones(false)
                .notificacionesPush(true)
                .notificacionesEmail(true)
                .notificacionesVisuales(true)
                .badgesDinamicos(true)
                .build();
    }

    private PreferenciaUsuario fromCommand(UUID usuarioId, ActualizarPreferenciaAdminCommand c) {
        return PreferenciaUsuario.builder()
                .usuarioId(usuarioId)
                .tema(c.getTema())
                .tipografia(c.getTipografia())
                .tamanioFuente(c.getTamanioFuente())
                .sonidoNotificaciones(c.isSonidoNotificaciones())
                .notificacionesPush(c.isNotificacionesPush())
                .notificacionesEmail(c.isNotificacionesEmail())
                .notificacionesVisuales(c.isNotificacionesVisuales())
                .badgesDinamicos(c.isBadgesDinamicos())
                .build();
    }

    private PreferenciaUsuario applyPatch(PreferenciaUsuario base, Map<String, Object> patch) {
        PreferenciaUsuario.PreferenciaUsuarioBuilder b = base.toBuilder();
        patch.forEach((key, value) -> {
            switch (key) {
                case "tema"                   -> b.tema(str(value));
                case "tipografia"              -> b.tipografia(str(value));
                case "tamanioFuente"           -> b.tamanioFuente(str(value));
                case "sonidoNotificaciones"    -> b.sonidoNotificaciones(bool(value));
                case "notificacionesPush"      -> b.notificacionesPush(bool(value));
                case "notificacionesEmail"     -> b.notificacionesEmail(bool(value));
                case "notificacionesVisuales"  -> b.notificacionesVisuales(bool(value));
                case "badgesDinamicos"         -> b.badgesDinamicos(bool(value));
                default -> { }
            }
        });
        return b.build();
    }

    private PreferenciaAdminResponse toResponse(PreferenciaUsuario p) {
        return PreferenciaAdminResponse.builder()
                .id(p.getUsuarioId().toString())
                .idUsuarioAdmin(p.getUsuarioId().toString())
                .tema(p.getTema())
                .tipografia(p.getTipografia())
                .tamanioFuente(p.getTamanioFuente())
                .sonidoNotificaciones(p.isSonidoNotificaciones())
                .notificacionesPush(p.isNotificacionesPush())
                .notificacionesEmail(p.isNotificacionesEmail())
                .notificacionesVisuales(p.isNotificacionesVisuales())
                .badgesDinamicos(p.isBadgesDinamicos())
                .fechaCreacion(p.getFechaCreacion() != null ? p.getFechaCreacion().toString() : null)
                .fechaActualizacion(p.getFechaActualizacion() != null ? p.getFechaActualizacion().toString() : null)
                .build();
    }

    private static boolean bool(Object v) {
        return v instanceof Boolean b ? b : false;
    }

    private static String str(Object v) {
        return v instanceof String s ? s : null;
    }
}
