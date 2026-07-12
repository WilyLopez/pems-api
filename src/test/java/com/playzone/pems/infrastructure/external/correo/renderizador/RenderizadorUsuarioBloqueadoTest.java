package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
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
class RenderizadorUsuarioBloqueadoTest {

    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;

    private RenderizadorUsuarioBloqueado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorUsuarioBloqueado(perfilUsuarioRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsUsuarioBloqueado() {
        assertEquals("USUARIO_BLOQUEADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaUsandoTituloYMensajeYaInterpolados() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                PerfilUsuario.builder().id(usuarioId).nombreCompleto("Pedro").correo("pedro@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioUsuarioId(usuarioId)
                .titulo("Cuenta bloqueada — Pedro")
                .mensaje("La cuenta de Pedro fue bloqueada. Motivo: Demasiados intentos fallidos.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("pedro@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Demasiados intentos fallidos"));
    }
}
