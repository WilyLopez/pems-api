package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorCambioPasswordTest {

    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;

    private RenderizadorCambioPassword renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorCambioPassword(perfilUsuarioRepository, clientePerfilRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsCambioPassword() {
        assertEquals("CAMBIO_PASSWORD", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaParaStaffUsandoDestinatarioUsuarioId() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                PerfilUsuario.builder().id(usuarioId).nombreCompleto("Ana").correo("ana@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioUsuarioId(usuarioId)
                .titulo("Tu contraseña fue actualizada")
                .mensaje("Tu contraseña se actualizó correctamente.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("ana@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Tu contraseña se actualizó correctamente."));
    }

    @Test
    void testRenderizaParaClienteUsandoDestinatarioClienteId() {
        when(clientePerfilRepository.buscarPorId(7L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(7L).nombres("Luis").correo("luis@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioClienteId(7L)
                .titulo("Tu contraseña fue actualizada")
                .mensaje("Tu contraseña se actualizó correctamente.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("luis@correo.com", resultado.getDestinatario());
    }
}
