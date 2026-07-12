package com.playzone.pems.application.usuario.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.application.usuario.port.out.SupabaseAuthPort;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.StaffPerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthContext;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private SupabaseAuthPort supabaseAuthPort;
    @Mock private StaffPerfilRepository staffPerfilRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private SupabaseAuthFacade supabaseAuthFacade;
    @Mock private ConfiguracionGlobalRepository configuracionGlobalRepository;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(
                supabaseAuthPort, staffPerfilRepository, clientePerfilRepository, supabaseAuthFacade,
                configuracionGlobalRepository, auditoria, crearNotificacionPort, resolverAdministradoresPort);
    }

    @Test
    void testCambiarPasswordDeStaffNotificaCambioPassword() {
        UUID usuarioId = UUID.randomUUID();
        SupabaseAuthContext ctx = new SupabaseAuthContext(
                usuarioId, "staff@correo.com", "authenticated", List.of("CAJERO"), List.of(), null, 1L, 0L);
        when(supabaseAuthFacade.contextoActual()).thenReturn(Optional.of(ctx));
        when(staffPerfilRepository.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.of(
                StaffPerfil.builder().id(1L).usuarioId(usuarioId).debeCambiarContrasena(true).build()));
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar("access-token", "actual123", "Nueva#123");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("CAMBIO_PASSWORD", captor.getValue().getTipoCodigo());
        assertEquals(usuarioId, captor.getValue().getDestinatarioUsuarioId());
        verify(clientePerfilRepository, never()).buscarPorUsuarioId(any());
    }

    @Test
    void testCambiarPasswordDeClienteNotificaCambioPassword() {
        UUID usuarioId = UUID.randomUUID();
        SupabaseAuthContext ctx = new SupabaseAuthContext(
                usuarioId, "cliente@correo.com", "authenticated", List.of("CLIENTE"), List.of(), 5L, null, 0L);
        when(supabaseAuthFacade.contextoActual()).thenReturn(Optional.of(ctx));
        when(staffPerfilRepository.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.empty());
        when(clientePerfilRepository.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.of(
                ClientePerfil.builder().id(5L).usuarioId(usuarioId).build()));

        service.ejecutar("access-token", "actual123", "Nueva#123");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("CAMBIO_PASSWORD", captor.getValue().getTipoCodigo());
        assertEquals(5L, captor.getValue().getDestinatarioClienteId());
    }

    @Test
    void testLoginFallidoAlAlcanzarMaximoNotificaBloqueoUnaVezYAAdministradores() {
        UUID usuarioId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(staffPerfilRepository.buscarPorCorreo("staff@correo.com")).thenReturn(Optional.of(
                StaffPerfil.builder().id(1L).usuarioId(usuarioId).esActivo(true).intentosFallidos(4).build()));
        when(supabaseAuthPort.login(anyString(), anyString()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null));
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(adminId));

        assertThrows(UnauthorizedException.class,
                () -> service.ejecutar("staff@correo.com", "wrong", "127.0.0.1", "agente"));

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort, times(2)).notificarTransaccional(captor.capture());
        List<CrearNotificacionCommand> comandos = captor.getAllValues();
        assertTrue(comandos.stream().allMatch(c -> "USUARIO_BLOQUEADO".equals(c.getTipoCodigo())));
        assertTrue(comandos.stream().anyMatch(c -> usuarioId.equals(c.getDestinatarioUsuarioId())));
        assertTrue(comandos.stream().anyMatch(c -> adminId.equals(c.getDestinatarioUsuarioId())));
    }

    @Test
    void testLoginFallidoAntesDelMaximoNoNotificaBloqueo() {
        UUID usuarioId = UUID.randomUUID();
        when(staffPerfilRepository.buscarPorCorreo("staff@correo.com")).thenReturn(Optional.of(
                StaffPerfil.builder().id(1L).usuarioId(usuarioId).esActivo(true).intentosFallidos(1).build()));
        when(supabaseAuthPort.login(anyString(), anyString()))
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], null));
        when(staffPerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(UnauthorizedException.class,
                () -> service.ejecutar("staff@correo.com", "wrong", "127.0.0.1", "agente"));

        verify(crearNotificacionPort, never()).notificarTransaccional(any());
        verify(resolverAdministradoresPort, never()).obtenerIdsAdministradoresActivos();
    }
}
