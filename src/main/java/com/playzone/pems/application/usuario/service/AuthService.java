package com.playzone.pems.application.usuario.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.application.usuario.port.in.CambiarPasswordMeUseCase;
import com.playzone.pems.application.usuario.port.in.LoginUseCase;
import com.playzone.pems.application.usuario.port.in.RecuperarPasswordUseCase;
import com.playzone.pems.application.usuario.port.out.SupabaseAuthPort;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.model.StaffPerfil;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthContext;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.UnauthorizedException;
import com.playzone.pems.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService implements LoginUseCase, RecuperarPasswordUseCase, CambiarPasswordMeUseCase {

    private final SupabaseAuthPort               supabaseAuthPort;
    private final StaffPerfilRepository          staffPerfilRepository;
    private final ClientePerfilRepository        clientePerfilRepository;
    private final SupabaseAuthFacade             supabaseAuthFacade;
    private final ConfiguracionGlobalRepository  configuracionGlobalRepository;
    private final RegistrarLogUseCase            auditoria;
    private final CrearNotificacionPort          crearNotificacionPort;
    private final ResolverAdministradoresPort    resolverAdministradoresPort;

    private int leerEntero(String clave, int defecto) {
        return configuracionGlobalRepository.findByClave(clave)
                .map(c -> { try { return Integer.parseInt(c.getValor()); } catch (NumberFormatException e) { return defecto; } })
                .orElse(defecto);
    }

    @Override
    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Map<String, Object> ejecutar(String email, String password, String ipOrigen, String userAgent) {
        String emailNorm = email.toLowerCase(java.util.Locale.ROOT).trim();
        Optional<StaffPerfil> staffOpt = staffPerfilRepository.buscarPorCorreo(emailNorm);

        if (staffOpt.isPresent()) {
            StaffPerfil staff = staffOpt.get();
            if (staff.getBloqueadoHasta() != null && staff.getBloqueadoHasta().isAfter(OffsetDateTime.now())) {
                throw new UnauthorizedException("Usuario bloqueado temporalmente. Intente nuevamente en unos minutos.");
            }
            if (!staff.isEsActivo()) {
                throw new UnauthorizedException("Cuenta desactivada. Contacte al administrador.");
            }
        }

        try {
            Map<String, Object> response = supabaseAuthPort.login(emailNorm, password);

            if (staffOpt.isPresent()) {
                StaffPerfil staff = staffOpt.get();
                staffPerfilRepository.guardar(staff.toBuilder()
                        .intentosFallidos(0)
                        .bloqueadoHasta(null)
                        .build());
                response.put("debeCambiarPassword", staff.isDebeCambiarContrasena());
                auditoria.ejecutar(new RegistrarLogUseCase.Command(
                        staff.getUsuarioId(), AuditoriaConstants.ACCION_LOGIN, AuditoriaConstants.MOD_ACCESOS,
                        "PerfilUsuario", null, null, null,
                        "Login exitoso: " + emailNorm,
                        ipOrigen, userAgent, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
            }

            return response;
        } catch (HttpClientErrorException e) {
            int maxIntentos        = leerEntero("INTENTOS_LOGIN_ANTES_BLOQUEO", 5);
            int duracionBloqueoMin = leerEntero("DURACION_BLOQUEO_LOGIN_MIN", 15);

            if (staffOpt.isPresent()) {
                StaffPerfil staff = staffOpt.get();
                int nuevosIntentos = (staff.getIntentosFallidos() != null ? staff.getIntentosFallidos() : 0) + 1;
                OffsetDateTime bloqueo = null;
                if (nuevosIntentos >= maxIntentos) {
                    bloqueo = OffsetDateTime.now().plusMinutes(duracionBloqueoMin);
                    log.warn("Usuario {} bloqueado hasta {}", emailNorm, bloqueo);
                }
                staffPerfilRepository.guardar(staff.toBuilder()
                        .intentosFallidos(nuevosIntentos)
                        .bloqueadoHasta(bloqueo)
                        .build());

                UUID userId = staff.getUsuarioId();
                auditoria.ejecutar(new RegistrarLogUseCase.Command(
                        userId, AuditoriaConstants.ACCION_LOGIN_FALLIDO, AuditoriaConstants.MOD_ACCESOS,
                        "PerfilUsuario", null, null, null,
                        "Intento fallido para: " + emailNorm,
                        ipOrigen, userAgent, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_FALLIDO));

                if (bloqueo != null) {
                    auditoria.ejecutar(new RegistrarLogUseCase.Command(
                            userId, AuditoriaConstants.ACCION_BLOQUEO, AuditoriaConstants.MOD_ACCESOS,
                            "PerfilUsuario", null, null, null,
                            "Cuenta bloqueada tras " + nuevosIntentos + " intentos fallidos",
                            ipOrigen, userAgent, AuditoriaConstants.NIVEL_CRITICAL, AuditoriaConstants.RESULTADO_FALLIDO));

                    notificarBloqueo(userId, emailNorm, "Demasiados intentos fallidos de inicio de sesión ("
                            + nuevosIntentos + ").");
                }
            }
            throw new UnauthorizedException("Credenciales invalidas.");
        }
    }

    @Override
    @Transactional
    public void ejecutar(String email) {
        supabaseAuthPort.recuperarPassword(email);
    }

    @Override
    @Transactional
    public void ejecutar(String accessToken, String passwordActual, String nuevoPassword) {
        SupabaseAuthContext ctx = supabaseAuthFacade.contextoActual()
                .orElseThrow(() -> new UnauthorizedException("No autenticado"));

        try {
            supabaseAuthPort.login(ctx.email(), passwordActual);
        } catch (Exception e) {
            throw new ValidationException("passwordActual", "La contraseña actual no es correcta.");
        }

        supabaseAuthPort.actualizarPassword(accessToken, nuevoPassword);

        Optional<StaffPerfil> staffOpt = staffPerfilRepository.buscarPorUsuarioId(ctx.userId());
        if (staffOpt.isPresent()) {
            StaffPerfil staff = staffOpt.get();
            if (staff.isDebeCambiarContrasena()) {
                staffPerfilRepository.guardar(staff.toBuilder()
                        .debeCambiarContrasena(false)
                        .build());
            }
            crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                    .tipoCodigo("CAMBIO_PASSWORD")
                    .destinatarioUsuarioId(ctx.userId())
                    .build());
        } else {
            clientePerfilRepository.buscarPorUsuarioId(ctx.userId()).ifPresent(cliente ->
                    crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                            .tipoCodigo("CAMBIO_PASSWORD")
                            .destinatarioClienteId(cliente.getId())
                            .build()));
        }
    }

    private void notificarBloqueo(UUID usuarioId, String nombre, String motivo) {
        Map<String, String> datos = Map.of("nombre", nombre, "motivo", motivo);

        crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("USUARIO_BLOQUEADO")
                .destinatarioUsuarioId(usuarioId)
                .datosExtra(datos)
                .build());

        for (UUID adminId : resolverAdministradoresPort.obtenerIdsAdministradoresActivos()) {
            crearNotificacionPort.notificarTransaccional(CrearNotificacionCommand.builder()
                    .tipoCodigo("USUARIO_BLOQUEADO")
                    .destinatarioUsuarioId(adminId)
                    .datosExtra(datos)
                    .build());
        }
    }
}
