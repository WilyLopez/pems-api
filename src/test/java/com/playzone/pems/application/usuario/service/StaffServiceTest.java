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
import java.util.Map;
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
    void testListarNoEjecutaConsultasPorFilaParaPerfilNiRoles() {
        UUID usuarioId1 = UUID.randomUUID();
        UUID usuarioId2 = UUID.randomUUID();
        StaffPerfil staff1 = StaffPerfil.builder().id(1L).usuarioId(usuarioId1).sedeId(1L).esActivo(true).build();
        StaffPerfil staff2 = StaffPerfil.builder().id(2L).usuarioId(usuarioId2).sedeId(1L).esActivo(true).build();
        when(staffPerfilRepository.listarTodos()).thenReturn(List.of(staff1, staff2));
        when(perfilUsuarioRepository.buscarPorIds(List.of(usuarioId1, usuarioId2))).thenReturn(List.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder().id(usuarioId1).nombreCompleto("Ana").build(),
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder().id(usuarioId2).nombreCompleto("Beto").build()));
        when(usuarioRolRepository.listarCodigosRolPorUsuarios(List.of(usuarioId1, usuarioId2))).thenReturn(Map.of(
                usuarioId1, List.of("ADMIN"),
                usuarioId2, List.of("CAJERO")));
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));

        List<UsuarioAdminResponse> respuesta = service.ejecutar();

        assertEquals(2, respuesta.size());
        verify(perfilUsuarioRepository, never()).buscarPorId(any());
        verify(usuarioRolRepository, never()).listarCodigosRolPorUsuario(any());
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

        ArgumentCaptor<RegistrarLogUseCase.Command> auditCaptor = ArgumentCaptor.forClass(RegistrarLogUseCase.Command.class);
        verify(auditoria).ejecutarSincrono(auditCaptor.capture());
        assertEquals("ACTIVAR_CUENTA", auditCaptor.getValue().accion());
        assertEquals(5L, auditCaptor.getValue().idEntidad());
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
        UUID actorId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(7L).usuarioId(usuarioId).esActivo(true).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("SUPERADMIN"));
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
        UUID actorId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(8L).usuarioId(usuarioId).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("SUPERADMIN"));
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

    @Test
    void testEditarPropioPerfilFunciona() {
        UUID usuarioId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(10L).usuarioId(usuarioId).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(usuarioId));
        when(staffPerfilRepository.buscarPorId(10L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(usuarioId)).thenReturn(List.of("CAJERO"));
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(usuarioId).nombreCompleto("Pedro Ruiz").build()));

        assertDoesNotThrow(() -> service.ejecutar(10L, "Pedro Ruiz Nuevo", "999999999"));
        verify(perfilUsuarioRepository).actualizarPerfil(usuarioId, "Pedro Ruiz Nuevo", "999999999");
    }

    @Test
    void testEditarPorAdminSobreOtroAdminLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(11L).usuarioId(objetivoId).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(11L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("ADMIN"));

        assertThrows(ValidationException.class, () -> service.ejecutar(11L, "Nombre", "999999999"));
        verify(perfilUsuarioRepository, never()).actualizarPerfil(any(), any(), any());
    }

    @Test
    void testEditarPorAdminSobreCajeroFunciona() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(12L).usuarioId(objetivoId).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(12L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("CAJERO"));
        when(perfilUsuarioRepository.buscarPorId(objetivoId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(objetivoId).nombreCompleto("Cajero Uno").build()));

        assertDoesNotThrow(() -> service.ejecutar(12L, "Nombre Nuevo", "999999999"));
        verify(perfilUsuarioRepository).actualizarPerfil(objetivoId, "Nombre Nuevo", "999999999");
    }

    @Test
    void testDesactivarSobrePropiaCuentaLanzaValidationException() {
        UUID usuarioId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(13L).usuarioId(usuarioId).esActivo(true).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(usuarioId));
        when(staffPerfilRepository.buscarPorId(13L)).thenReturn(Optional.of(staff));

        assertThrows(ValidationException.class, () -> service.desactivar(13L));
        verify(staffPerfilRepository, never()).guardar(any());
    }

    @Test
    void testDesactivarPorAdminSobreOtroAdminLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(14L).usuarioId(objetivoId).esActivo(true).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(14L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("ADMIN"));

        assertThrows(ValidationException.class, () -> service.desactivar(14L));
        verify(staffPerfilRepository, never()).guardar(any());
    }

    @Test
    void testActivarPorAdminSobreOtroAdminLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(15L).usuarioId(objetivoId).esActivo(false).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(15L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("ADMIN"));

        assertThrows(ValidationException.class, () -> service.activar(15L));
        verify(staffPerfilRepository, never()).guardar(any());
    }

    @Test
    void testActivarConCupoDeAdminsAlcanzadoLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(23L).usuarioId(objetivoId).esActivo(false).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(23L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("SUPERADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("ADMIN"));
        when(staffPerfilRepository.contarActivosPorRol("ADMIN")).thenReturn(4L);

        assertThrows(ValidationException.class, () -> service.activar(23L));
        verify(staffPerfilRepository, never()).guardar(any());
    }

    @Test
    void testActivarConCupoDisponibleActivaCorrectamente() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(24L).usuarioId(objetivoId).esActivo(false).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(24L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("SUPERADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("CAJERO"));
        when(staffPerfilRepository.contarActivosPorRol("CAJERO")).thenReturn(1L);
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.activar(24L));
        verify(staffPerfilRepository).guardar(any());
    }

    @Test
    void testResetearSobrePropiaCuentaLanzaValidationException() {
        UUID usuarioId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(16L).usuarioId(usuarioId).esActivo(true).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(usuarioId));
        when(staffPerfilRepository.buscarPorId(16L)).thenReturn(Optional.of(staff));

        assertThrows(ValidationException.class, () -> service.resetear(16L));
        verify(supabaseAuthPort, never()).recuperarPassword(anyString());
    }

    @Test
    void testResetearPorAdminSobreOtroAdminLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(17L).usuarioId(objetivoId).esActivo(true).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(17L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("ADMIN"));

        assertThrows(ValidationException.class, () -> service.resetear(17L));
        verify(supabaseAuthPort, never()).recuperarPassword(anyString());
    }

    @Test
    void testResetearConAdminSobreCajeroRegistraAuditoria() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(22L).usuarioId(objetivoId).esActivo(true).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(22L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("CAJERO"));
        when(perfilUsuarioRepository.buscarPorId(objetivoId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(objetivoId).correo("cajero@correo.com").build()));

        service.resetear(22L);

        verify(supabaseAuthPort).recuperarPassword("cajero@correo.com");

        ArgumentCaptor<RegistrarLogUseCase.Command> auditCaptor = ArgumentCaptor.forClass(RegistrarLogUseCase.Command.class);
        verify(auditoria).ejecutarSincrono(auditCaptor.capture());
        assertEquals("RESETEAR_PASSWORD", auditCaptor.getValue().accion());
        assertEquals(22L, auditCaptor.getValue().idEntidad());
    }

    @Test
    void testDesbloquearSobrePropiaCuentaLanzaValidationException() {
        UUID usuarioId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(18L).usuarioId(usuarioId).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(usuarioId));
        when(staffPerfilRepository.buscarPorId(18L)).thenReturn(Optional.of(staff));

        assertThrows(ValidationException.class, () -> service.desbloquear(18L));
        verify(staffPerfilRepository, never()).guardar(any());
    }

    @Test
    void testDesbloquearPorAdminSobreOtroAdminLanzaValidationException() {
        UUID actorId = UUID.randomUUID();
        UUID objetivoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(19L).usuarioId(objetivoId).build();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(actorId));
        when(staffPerfilRepository.buscarPorId(19L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(actorId)).thenReturn(List.of("ADMIN"));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(objetivoId)).thenReturn(List.of("ADMIN"));

        assertThrows(ValidationException.class, () -> service.desbloquear(19L));
        verify(staffPerfilRepository, never()).guardar(any());
    }

    @Test
    void testCambiarRolPorAdminSobreOtroAdminLanzaValidationException() {
        UUID solicitanteId = UUID.randomUUID();
        UUID afectadoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(20L).usuarioId(afectadoId).build();
        when(usuarioRolRepository.listarCodigosRolPorUsuario(solicitanteId)).thenReturn(List.of("ADMIN"));
        when(staffPerfilRepository.buscarPorId(20L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(afectadoId)).thenReturn(List.of("ADMIN"));

        assertThrows(ValidationException.class, () -> service.ejecutar(20L, "CAJERO", solicitanteId));
        verify(usuarioRolRepository, never()).guardar(any());
    }

    @Test
    void testCambiarRolPorSuperadminSobreOtroSuperadminFunciona() {
        UUID solicitanteId = UUID.randomUUID();
        UUID afectadoId = UUID.randomUUID();
        StaffPerfil staff = StaffPerfil.builder().id(21L).usuarioId(afectadoId).build();
        when(usuarioRolRepository.listarCodigosRolPorUsuario(solicitanteId)).thenReturn(List.of("SUPERADMIN"));
        when(staffPerfilRepository.buscarPorId(21L)).thenReturn(Optional.of(staff));
        when(usuarioRolRepository.listarCodigosRolPorUsuario(afectadoId)).thenReturn(List.of("SUPERADMIN"));
        when(staffPerfilRepository.contarActivosPorRolExcluyendo("ADMIN", 21L)).thenReturn(0L);
        when(perfilUsuarioRepository.buscarPorId(afectadoId)).thenReturn(Optional.of(
                com.playzone.pems.domain.usuario.model.PerfilUsuario.builder()
                        .id(afectadoId).nombreCompleto("Otro Superadmin").build()));
        when(sedeRepository.findById(any())).thenReturn(Optional.empty());
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        assertDoesNotThrow(() -> service.ejecutar(21L, "ADMIN", solicitanteId));
        verify(usuarioRolRepository).guardar(any());
    }
}
