package com.playzone.pems.application.usuario.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.usuario.dto.command.ActualizarClientePerfilCommand;
import com.playzone.pems.application.usuario.dto.command.RegistrarClientePublicoCommand;
import com.playzone.pems.application.usuario.port.out.SupabaseAuthPort;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.ClienteToken;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.ClienteTokenRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.TokenHasher;
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
class ClientePerfilServiceTest {

    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private SupabaseAuthPort supabaseAuthPort;
    @Mock private ClienteTokenRepository clienteTokenRepository;
    @Mock private CrearNotificacionPort crearNotificacionPort;

    private ClientePerfilService clientePerfilService;

    private RegistrarClientePublicoCommand command;

    @BeforeEach
    void setUp() {
        clientePerfilService = new ClientePerfilService(
                clientePerfilRepository, perfilUsuarioRepository, supabaseAuthPort,
                clienteTokenRepository, crearNotificacionPort, new ObjectMapper());

        command = RegistrarClientePublicoCommand.builder()
                .nombre("Juan Perez")
                .correo("juan@gmail.com")
                .password("123456")
                .telefono("999888777")
                .tipoDocumentoCodigo("DNI")
                .numeroDocumento("12345678")
                .build();
    }

    @Test
    void registrarNuevoClienteCorrectamente() {
        UUID usuarioId = UUID.randomUUID();
        when(clientePerfilRepository.buscarPorCorreo(anyString())).thenReturn(Optional.empty());
        when(clientePerfilRepository.buscarPorDocumento(anyString(), anyString())).thenReturn(Optional.empty());
        when(supabaseAuthPort.crearUsuario(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(usuarioId);
        when(clientePerfilRepository.guardar(any(ClientePerfil.class))).thenAnswer(i -> i.getArgument(0));

        ClientePerfil resultado = clientePerfilService.ejecutar(command);

        assertNotNull(resultado);
        assertEquals(usuarioId, resultado.getUsuarioId());
        assertEquals(command.getCorreo(), resultado.getCorreo());
        assertEquals("WEB", resultado.getOrigen());
        verify(supabaseAuthPort).crearUsuario(command.getCorreo(), command.getPassword(), command.getNombre(), false);
        verify(clientePerfilRepository).guardar(any(ClientePerfil.class));
    }

    @Test
    void vincularClientePOSExtistente() {
        UUID usuarioId = UUID.randomUUID();
        ClientePerfil clienteExistente = ClientePerfil.builder()
                .id(1L)
                .correo(command.getCorreo())
                .usuarioId(null)
                .tipoDocumentoCodigo("DNI")
                .numeroDocumento("12345678")
                .build();

        when(clientePerfilRepository.buscarPorCorreo(command.getCorreo())).thenReturn(Optional.of(clienteExistente));
        when(supabaseAuthPort.crearUsuario(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(usuarioId);
        when(clientePerfilRepository.guardar(any(ClientePerfil.class))).thenAnswer(i -> i.getArgument(0));

        ClientePerfil resultado = clientePerfilService.ejecutar(command);

        assertNotNull(resultado);
        assertEquals(usuarioId, resultado.getUsuarioId());
        assertEquals(1L, resultado.getId());
        assertEquals("WEB", resultado.getOrigen());
        verify(supabaseAuthPort).crearUsuario(command.getCorreo(), command.getPassword(), command.getNombre(), false);
        verify(clientePerfilRepository, never()).buscarPorDocumento(anyString(), anyString());
    }

    @Test
    void fallarSiCorreoYaEstaVinculado() {
        ClientePerfil clienteExistente = ClientePerfil.builder()
                .id(1L)
                .correo(command.getCorreo())
                .usuarioId(UUID.randomUUID())
                .build();

        when(clientePerfilRepository.buscarPorCorreo(command.getCorreo())).thenReturn(Optional.of(clienteExistente));

        assertThrows(ValidationException.class, () -> clientePerfilService.ejecutar(command));
    }

    @Test
    void fallarSiDocumentoDuplicado() {
        when(clientePerfilRepository.buscarPorCorreo(anyString())).thenReturn(Optional.empty());
        when(clientePerfilRepository.buscarPorDocumento(anyString(), anyString()))
                .thenReturn(Optional.of(ClientePerfil.builder().build()));

        assertThrows(ValidationException.class, () -> clientePerfilService.ejecutar(command));
    }

    @Test
    void fallarSiSupabaseError() {
        when(clientePerfilRepository.buscarPorCorreo(anyString())).thenReturn(Optional.empty());
        when(clientePerfilRepository.buscarPorDocumento(anyString(), anyString())).thenReturn(Optional.empty());
        when(supabaseAuthPort.crearUsuario(anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Supabase Error"));

        assertThrows(RuntimeException.class, () -> clientePerfilService.ejecutar(command));
    }

    private ClientePerfil clienteConCorreo(String correo) {
        return ClientePerfil.builder().id(10L).nombres("Ana").correo(correo).build();
    }

    @Test
    void testActualizarCambiandoCorreoNoLoAplicaDeInmediatoYEnviaDosNotificaciones() {
        ClientePerfil existente = clienteConCorreo("actual@correo.com");
        when(clientePerfilRepository.buscarPorId(10L)).thenReturn(Optional.of(existente));
        when(clientePerfilRepository.buscarPorCorreo("nuevo@correo.com")).thenReturn(Optional.empty());
        when(clientePerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        ActualizarClientePerfilCommand comando = ActualizarClientePerfilCommand.builder()
                .correo("nuevo@correo.com")
                .build();

        ClientePerfil resultado = clientePerfilService.ejecutar(10L, comando);

        assertEquals("actual@correo.com", resultado.getCorreo());

        verify(clienteTokenRepository).guardar(any(ClienteToken.class));

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort, times(2)).notificarTransaccional(captor.capture());
        List<CrearNotificacionCommand> comandos = captor.getAllValues();

        assertTrue(comandos.stream().anyMatch(c -> "CAMBIO_CORREO_SOLICITADO".equals(c.getTipoCodigo())
                && c.getMetadata() != null && c.getMetadata().contains("nuevo@correo.com")));
        assertTrue(comandos.stream().anyMatch(c -> "CAMBIO_CORREO_ALERTA".equals(c.getTipoCodigo())));
    }

    @Test
    void testActualizarConMismoCorreoNoDisparaVerificacion() {
        ClientePerfil existente = clienteConCorreo("actual@correo.com");
        when(clientePerfilRepository.buscarPorId(10L)).thenReturn(Optional.of(existente));
        when(clientePerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        ActualizarClientePerfilCommand comando = ActualizarClientePerfilCommand.builder()
                .correo("ACTUAL@correo.com")
                .build();

        clientePerfilService.ejecutar(10L, comando);

        verify(clienteTokenRepository, never()).guardar(any());
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    @Test
    void testActualizarConCorreoYaEnUsoLanzaValidationException() {
        ClientePerfil existente = clienteConCorreo("actual@correo.com");
        when(clientePerfilRepository.buscarPorId(10L)).thenReturn(Optional.of(existente));
        when(clientePerfilRepository.buscarPorCorreo("nuevo@correo.com")).thenReturn(Optional.of(
                ClientePerfil.builder().id(99L).correo("nuevo@correo.com").build()));

        ActualizarClientePerfilCommand comando = ActualizarClientePerfilCommand.builder()
                .correo("nuevo@correo.com")
                .build();

        assertThrows(ValidationException.class, () -> clientePerfilService.ejecutar(10L, comando));
        verify(clientePerfilRepository, never()).guardar(any());
    }

    @Test
    void testConfirmarConTokenValidoAplicaElCorreoYNotifica() {
        ClienteToken token = ClienteToken.builder()
                .id(1L).clienteId(10L)
                .tokenHash(TokenHasher.hashear("token-correo"))
                .tipo("VERIFICAR_CORREO")
                .metadata("{\"correoNuevo\":\"nuevo@correo.com\"}")
                .expiraAt(OffsetDateTime.now().plusHours(12))
                .build();
        when(clienteTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));
        when(clientePerfilRepository.buscarPorCorreo("nuevo@correo.com")).thenReturn(Optional.empty());
        when(clientePerfilRepository.buscarPorId(10L)).thenReturn(Optional.of(clienteConCorreo("actual@correo.com")));
        when(clientePerfilRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clienteTokenRepository.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

        clientePerfilService.confirmar(10L, "token-correo");

        ArgumentCaptor<ClientePerfil> clienteCaptor = ArgumentCaptor.forClass(ClientePerfil.class);
        verify(clientePerfilRepository).guardar(clienteCaptor.capture());
        assertEquals("nuevo@correo.com", clienteCaptor.getValue().getCorreo());

        ArgumentCaptor<ClienteToken> tokenCaptor = ArgumentCaptor.forClass(ClienteToken.class);
        verify(clienteTokenRepository).guardar(tokenCaptor.capture());
        assertNotNull(tokenCaptor.getValue().getUsadoAt());

        ArgumentCaptor<CrearNotificacionCommand> notifCaptor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(notifCaptor.capture());
        assertEquals("CAMBIO_CORREO_CONFIRMADO", notifCaptor.getValue().getTipoCodigo());
    }

    @Test
    void testConfirmarConTokenDeOtroClienteLanzaValidationException() {
        ClienteToken token = ClienteToken.builder()
                .id(1L).clienteId(10L)
                .tokenHash(TokenHasher.hashear("token-correo"))
                .tipo("VERIFICAR_CORREO")
                .metadata("{\"correoNuevo\":\"nuevo@correo.com\"}")
                .expiraAt(OffsetDateTime.now().plusHours(12))
                .build();
        when(clienteTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(ValidationException.class, () -> clientePerfilService.confirmar(99L, "token-correo"));
        verify(clientePerfilRepository, never()).guardar(any());
    }

    @Test
    void testConfirmarConTokenExpiradoLanzaValidationException() {
        ClienteToken token = ClienteToken.builder()
                .id(1L).clienteId(10L)
                .tokenHash(TokenHasher.hashear("token-correo"))
                .tipo("VERIFICAR_CORREO")
                .metadata("{\"correoNuevo\":\"nuevo@correo.com\"}")
                .expiraAt(OffsetDateTime.now().minusHours(1))
                .build();
        when(clienteTokenRepository.buscarPorTokenHash(anyString())).thenReturn(Optional.of(token));

        assertThrows(ValidationException.class, () -> clientePerfilService.confirmar(10L, "token-correo"));
        verify(clientePerfilRepository, never()).guardar(any());
    }
}
