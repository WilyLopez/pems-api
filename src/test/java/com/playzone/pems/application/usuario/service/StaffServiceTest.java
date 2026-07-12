package com.playzone.pems.application.usuario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.application.usuario.dto.command.RegistrarUsuarioAdminCommand;
import com.playzone.pems.application.usuario.dto.response.UsuarioAdminResponse;
import com.playzone.pems.application.usuario.port.out.SupabaseAuthPort;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.model.StaffPerfil;
import com.playzone.pems.domain.usuario.model.StaffToken;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.domain.usuario.repository.StaffTokenRepository;
import com.playzone.pems.domain.usuario.repository.UsuarioRolRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaffServiceTest {

    @Mock private StaffPerfilRepository staffPerfilRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioRolRepository usuarioRolRepository;
    @Mock private StaffTokenRepository staffTokenRepository;
    @Mock private SupabaseAuthPort supabaseAuthPort;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;

    private StaffService service;

    @BeforeEach
    void setUp() {
        service = new StaffService(
                staffPerfilRepository, perfilUsuarioRepository, sedeRepository, usuarioRolRepository,
                staffTokenRepository, supabaseAuthPort, crearNotificacionPort, resolverAdministradoresPort,
                authFacade, auditoria, new ObjectMapper());
    }

    private RegistrarUsuarioAdminCommand comandoCajero() {
        return RegistrarUsuarioAdminCommand.builder()
                .nombre("Maria Lopez").correo("Maria@Correo.com").rolCodigo("CAJERO").sedeId(1L).build();
    }

    @Test
    void testRegistrarStaffCreaTokenDeActivacionYNotificaTransaccional() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorCorreo("maria@correo.com")).thenReturn(Optional.empty());
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));
        when(supabaseAuthPort.crearUsuario(eq("maria@correo.com"), any(), eq("Maria Lopez"), eq(true)))
                .thenReturn(usuarioId);
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> {
            StaffPerfil arg = inv.getArgument(0);
            return arg.toBuilder().id(100L).build();
        });
        when(authFacade.usuarioActualId()).thenReturn(Optional.empty());

        UsuarioAdminResponse response = service.ejecutar(comandoCajero());

        assertEquals(100L, response.getId());
        assertEquals(usuarioId, response.getUsuarioId());
        assertTrue(response.isDebeCambiarContrasena());

        ArgumentCaptor<StaffToken> tokenCaptor = ArgumentCaptor.forClass(StaffToken.class);
        verify(staffTokenRepository).guardar(tokenCaptor.capture());
        StaffToken tokenGuardado = tokenCaptor.getValue();
        assertEquals(usuarioId, tokenGuardado.getUsuarioId());
        assertEquals("ACTIVACION_CUENTA", tokenGuardado.getTipo());
        assertNull(tokenGuardado.getUsadoAt());
        assertTrue(tokenGuardado.getExpiraAt().isAfter(OffsetDateTime.now().plusHours(23)));
        assertTrue(tokenGuardado.getExpiraAt().isBefore(OffsetDateTime.now().plusHours(25)));

        ArgumentCaptor<CrearNotificacionCommand> cmdCaptor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(cmdCaptor.capture());
        CrearNotificacionCommand cmd = cmdCaptor.getValue();
        assertEquals("USUARIO_ACTIVACION", cmd.getTipoCodigo());
        assertEquals(usuarioId, cmd.getDestinatarioUsuarioId());
        assertEquals(100L, cmd.getEntidadId());
        assertTrue(cmd.getMetadata().contains("tokenActivacion"));
        verify(crearNotificacionPort, never()).notificar(any());
    }

    @Test
    void testRegistrarAdminSinSerSuperadminLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));

        RegistrarUsuarioAdminCommand comando = RegistrarUsuarioAdminCommand.builder()
                .nombre("Nuevo Admin").correo("admin2@correo.com").rolCodigo("ADMIN").sedeId(1L).build();

        assertThrows(ValidationException.class, () -> service.ejecutar(comando));
        verify(staffTokenRepository, never()).guardar(any());
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    private StaffToken tokenVigente() {
        return StaffToken.builder()
                .id(1L).usuarioId(UUID.randomUUID()).tokenHash(com.playzone.pems.shared.util.TokenHasher.hashear("token-valido"))
                .tipo("ACTIVACION_CUENTA")
                .expiraAt(OffsetDateTime.now().plusHours(12))
                .build();
    }

    @Test
    void testActivarCuentaConTokenValidoEstablecePasswordYMarcaTokenUsado() {
        StaffToken token = tokenVigente();
        when(staffTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));
        when(staffPerfilRepository.buscarPorUsuarioId(token.getUsuarioId())).thenReturn(Optional.of(
                StaffPerfil.builder().id(5L).usuarioId(token.getUsuarioId()).debeCambiarContrasena(true).build()));
        when(staffTokenRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.activarCuenta("token-valido", "Segura#123");

        verify(supabaseAuthPort).establecerPasswordAdmin(token.getUsuarioId(), "Segura#123");

        ArgumentCaptor<StaffToken> tokenCaptor = ArgumentCaptor.forClass(StaffToken.class);
        verify(staffTokenRepository).guardar(tokenCaptor.capture());
        assertNotNull(tokenCaptor.getValue().getUsadoAt());

        ArgumentCaptor<StaffPerfil> staffCaptor = ArgumentCaptor.forClass(StaffPerfil.class);
        verify(staffPerfilRepository).guardar(staffCaptor.capture());
        assertFalse(staffCaptor.getValue().isDebeCambiarContrasena());
    }

    @Test
    void testActivarCuentaConTokenExpiradoLanzaValidationException() {
        StaffToken expirado = tokenVigente().toBuilder().expiraAt(OffsetDateTime.now().minusHours(1)).build();
        when(staffTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(expirado));

        assertThrows(ValidationException.class, () -> service.activarCuenta("token-valido", "Segura#123"));
        verify(supabaseAuthPort, never()).establecerPasswordAdmin(any(), anyString());
    }

    @Test
    void testActivarCuentaConTokenYaUsadoLanzaValidationException() {
        StaffToken usado = tokenVigente().toBuilder().usadoAt(OffsetDateTime.now().minusMinutes(5)).build();
        when(staffTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(usado));

        assertThrows(ValidationException.class, () -> service.activarCuenta("token-valido", "Segura#123"));
        verify(supabaseAuthPort, never()).establecerPasswordAdmin(any(), anyString());
    }

    @Test
    void testActivarCuentaConTokenInexistenteLanzaValidationException() {
        when(staffTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> service.activarCuenta("token-desconocido", "Segura#123"));
    }

    @Test
    void testActivarCuentaConPasswordDebilLanzaValidationException() {
        StaffToken token = tokenVigente();
        when(staffTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(ValidationException.class, () -> service.activarCuenta("token-valido", "debil"));
        verify(supabaseAuthPort, never()).establecerPasswordAdmin(any(), anyString());
    }

    @Test
    void testDesactivarNotificaAlStaffYAAdministradores() {
        UUID usuarioId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(7L).usuarioId(usuarioId).esActivo(true).build();
        when(staffPerfilRepository.buscarPorId(7L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(usuarioId)).thenReturn(List.of("CAJERO"));
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(usuarioId).nombreCompleto("Pedro Ruiz").build()));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(adminId));

        service.desactivar(7L);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort, times(2)).notificarTransaccional(captor.capture());
        List<CrearNotificacionCommand> comandos = captor.getAllValues();

        assertTrue(comandos.stream().allMatch(c -> "USUARIO_BLOQUEADO".equals(c.getTipoCodigo())));
        assertTrue(comandos.stream().anyMatch(c -> usuarioId.equals(c.getDestinatarioUsuarioId())));
        assertTrue(comandos.stream().anyMatch(c -> adminId.equals(c.getDestinatarioUsuarioId())));
    }

    @Test
    void testDesbloquearNotificaAlStaffYAAdministradores() {
        UUID usuarioId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(8L).usuarioId(usuarioId).build();
        when(staffPerfilRepository.buscarPorId(8L)).thenReturn(Optional.of(staff));
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(usuarioId).nombreCompleto("Pedro Ruiz").build()));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        service.desbloquear(8L);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("USUARIO_DESBLOQUEADO", captor.getValue().getTipoCodigo());
        assertEquals(usuarioId, captor.getValue().getDestinatarioUsuarioId());
    }

    @Test
    void testCambiarRolNotificaAlStaffConRolAnteriorYNuevo() {
        UUID solicitanteId = UUID.randomUUID();
        UUID afectadoId = UUID.randomUUID();
        when(usuarioRolRepository.listarCodigosRolPorUsuario(solicitanteId)).thenReturn(List.of("SUPERADMIN"));
        StaffPerfil staff = StaffPerfil.builder().id(9L).usuarioId(afectadoId).build();
        when(staffPerfilRepository.buscarPorId(9L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(afectadoId)).thenReturn(List.of("CAJERO"));
        when(staffPerfilRepository.contarActivosPorRolExcluyendo("ADMIN", 9L)).thenReturn(0L);
        when(perfilUsuarioRepository.buscarPorId(afectadoId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(afectadoId).nombreCompleto("Lucia Vega").build()));
        when(sedeRepository.findById(any())).thenReturn(Optional.empty());
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        service.ejecutar(9L, "ADMIN", solicitanteId);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("CAMBIO_ROL", cmd.getTipoCodigo());
        assertEquals("CAJERO", cmd.getDatosExtra().get("rolAnterior"));
        assertEquals("ADMIN", cmd.getDatosExtra().get("rolNuevo"));
    }
}
