package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RenderizadorCambioCorreoSolicitadoTest {

    private RenderizadorCambioCorreoSolicitado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorCambioCorreoSolicitado(new TemplateService(), new ObjectMapper());
    }

    @Test
    void testTipoCodigoEsCambioCorreoSolicitado() {
        assertEquals("CAMBIO_CORREO_SOLICITADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaEnviandoAlCorreoNuevoConEnlaceDeConfirmacion() {
        Notificacion notificacion = Notificacion.builder()
                .metadata("{\"correoNuevo\":\"nuevo@correo.com\",\"tokenCorreo\":\"raw-token-abc\"}")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("nuevo@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("token=raw-token-abc"));
        assertTrue(resultado.getCuerpoHtml().contains("nuevo@correo.com"));
    }

    @Test
    void testRenderizarSinMetadataLanzaExcepcion() {
        Notificacion notificacion = Notificacion.builder().build();

        assertThrows(IllegalStateException.class, () -> renderizador.renderizar(notificacion));
    }
}
