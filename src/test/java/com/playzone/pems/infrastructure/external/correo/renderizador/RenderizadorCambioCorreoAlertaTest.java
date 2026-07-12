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
class RenderizadorCambioCorreoAlertaTest {

    @Mock private ClientePerfilRepository clientePerfilRepository;

    private RenderizadorCambioCorreoAlerta renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorCambioCorreoAlerta(clientePerfilRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsCambioCorreoAlerta() {
        assertEquals("CAMBIO_CORREO_ALERTA", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaEnviandoAlCorreoActualDelCliente() {
        when(clientePerfilRepository.buscarPorId(3L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(3L).nombres("Marta").correo("actual@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .destinatarioClienteId(3L)
                .titulo("Solicitud de cambio de correo")
                .mensaje("Se solicitó cambiar el correo de contacto de tu cuenta a nuevo@correo.com.")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("actual@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("nuevo@correo.com"));
    }
}
