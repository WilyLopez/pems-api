package com.playzone.pems.application.cms.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.cms.port.in.GestionarMensajeContactoUseCase;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.cms.model.MensajeContacto;
import com.playzone.pems.domain.cms.repository.MensajeContactoRepository;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.external.correo.JavaMailCorreoClient;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MensajeContactoServiceTest {

    @Mock private MensajeContactoRepository repository;
    @Mock private JavaMailCorreoClient correoClient;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;

    private MensajeContactoService service;

    @BeforeEach
    void setUp() {
        service = new MensajeContactoService(
                repository, correoClient, resolverAdministradoresPort, perfilUsuarioRepository, authFacade, auditoria);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private GestionarMensajeContactoUseCase.RegistrarCommand comandoValido(String nombre, String mensaje) {
        return GestionarMensajeContactoUseCase.RegistrarCommand.builder()
                .nombre(nombre)
                .correo("cliente@correo.com")
                .telefono("999999999")
                .asunto("Consulta")
                .mensaje(mensaje)
                .ipOrigen("203.0.113.5")
                .userAgent("test-agent")
                .build();
    }

    @Test
    void testRegistrarEnviaAlertaAAdministradoresActivos() {
        UUID idAdmin = UUID.randomUUID();
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(idAdmin));
        when(perfilUsuarioRepository.buscarPorId(idAdmin)).thenReturn(
                Optional.of(PerfilUsuario.builder().id(idAdmin).correo("admin@kikiylala.lat").build()));

        service.registrar(comandoValido("Juan Perez", "Hola, tengo una consulta"));

        verify(correoClient).enviar(eq("admin@kikiylala.lat"), anyString(), anyString());
    }

    @Test
    void testRegistrarEscapaHtmlEnCamposDelCliente() {
        UUID idAdmin = UUID.randomUUID();
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(idAdmin));
        when(perfilUsuarioRepository.buscarPorId(idAdmin)).thenReturn(
                Optional.of(PerfilUsuario.builder().id(idAdmin).correo("admin@kikiylala.lat").build()));

        service.registrar(comandoValido(
                "<img src=x onerror=alert(1)>", "<script>document.location='http://evil.com'</script>"));

        ArgumentCaptor<String> cuerpoCaptor = ArgumentCaptor.forClass(String.class);
        verify(correoClient).enviar(eq("admin@kikiylala.lat"), anyString(), cuerpoCaptor.capture());

        String cuerpoEnviado = cuerpoCaptor.getValue();
        assertFalse(cuerpoEnviado.contains("<img src=x onerror=alert(1)>"));
        assertFalse(cuerpoEnviado.contains("<script>document.location='http://evil.com'</script>"));
        assertTrue(cuerpoEnviado.contains("&lt;img"));
        assertTrue(cuerpoEnviado.contains("&lt;script&gt;"));
    }

    @Test
    void testRegistrarSinAdministradoresNoEnviaNiFalla() {
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        MensajeContacto resultado = assertDoesNotThrow(
                () -> service.registrar(comandoValido("Juan Perez", "Hola")));

        assertNotNull(resultado);
        verify(correoClient, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void testResponderEscapaHtmlEnDatosDelClienteYDeLaRespuesta() {
        MensajeContacto mensaje = MensajeContacto.builder()
                .id(1L)
                .nombre("<b onmouseover=alert(1)>Cliente</b>")
                .correo("cliente@correo.com")
                .mensaje("<svg onload=alert(2)>")
                .estado("PENDIENTE")
                .build();
        when(repository.findById(1L)).thenReturn(Optional.of(mensaje));

        GestionarMensajeContactoUseCase.ResponderCommand cmd = GestionarMensajeContactoUseCase.ResponderCommand.builder()
                .idMensaje(1L)
                .respuesta("<iframe src=javascript:alert(3)>")
                .idUsuarioAdmin(UUID.randomUUID())
                .build();

        service.responder(cmd);

        ArgumentCaptor<String> cuerpoCaptor = ArgumentCaptor.forClass(String.class);
        verify(correoClient).enviar(eq("cliente@correo.com"), anyString(), cuerpoCaptor.capture());

        String cuerpoEnviado = cuerpoCaptor.getValue();
        assertFalse(cuerpoEnviado.contains("<b onmouseover=alert(1)>"));
        assertFalse(cuerpoEnviado.contains("<svg onload=alert(2)>"));
        assertFalse(cuerpoEnviado.contains("<iframe src=javascript:alert(3)>"));
    }
}
