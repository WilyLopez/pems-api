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
class RenderizadorUsuarioDesbloqueadoTest {

    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;

    private RenderizadorUsuarioDesbloqueado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorUsuarioDesbloqueado(perfilUsuarioRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsUsuarioDesbloqueado() {
        assertEquals("USUARIO_DESBLOQUEADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaUsandoCorreoDelStaff() {
        UUID usuarioId = UUID.randomUUID();
        when(perfilUsuarioRepository.buscarPorId(usuarioId)).thenReturn(Optional.of(
                PerfilUsuario.builder().id(usuarioId).nombreCompleto("Pedro").correo("pedro@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioUsuarioId(usuarioId)
                .titulo("Cuenta desbloqueada — Pedro")
                .mensaje("La cuenta de Pedro fue desbloqueada.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("pedro@correo.com", resultado.getDestinatario());
    }
}
