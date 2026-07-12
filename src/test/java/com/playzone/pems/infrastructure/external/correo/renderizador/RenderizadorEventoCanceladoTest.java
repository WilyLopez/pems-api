package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorEventoCanceladoTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;

    private RenderizadorEventoCancelado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorEventoCancelado(
                eventoRepository, clientePerfilRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsEventoCanceladoAdmin() {
        assertEquals("EVENTO_CANCELADO_ADMIN", renderizador.tipoCodigo());
    }

    @Test
    void testUsaMotivoCancelacionPersistidoEnElEvento() {
        when(eventoRepository.findById(80L)).thenReturn(Optional.of(EventoPrivado.builder()
                .id(80L).idCliente(11L)
                .fechaEvento(LocalDate.of(2026, 12, 1))
                .motivoCancelacion("<b onmouseover=alert(1)>Local en mantenimiento</b>")
                .build()));
        when(clientePerfilRepository.buscarPorId(11L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(11L).nombres("Carla").correo("carla@correo.com").build()));

        Notificacion notificacion = Notificacion.builder().entidadId(80L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("carla@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Local en mantenimiento"));
        assertFalse(resultado.getCuerpoHtml().contains("<b onmouseover=alert(1)>"));
    }
}
