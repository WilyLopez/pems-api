package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorCambioCorreoConfirmadoTest {

    @Mock private ClientePerfilRepository clientePerfilRepository;

    private RenderizadorCambioCorreoConfirmado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorCambioCorreoConfirmado(clientePerfilRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsCambioCorreoConfirmado() {
        assertEquals("CAMBIO_CORREO_CONFIRMADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaEnviandoAlCorreoYaActualizado() {
        when(clientePerfilRepository.buscarPorId(3L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(3L).nombres("Marta").correo("nuevo@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioClienteId(3L)
                .titulo("Correo actualizado")
                .mensaje("Tu correo de contacto se actualizó correctamente a nuevo@correo.com.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("nuevo@correo.com", resultado.getDestinatario());
    }
}
