package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
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
class RenderizadorEventoRecordatorio3DiasTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private SedeRepository sedeRepository;

    private RenderizadorEventoRecordatorio3Dias renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorEventoRecordatorio3Dias(
                eventoRepository, clientePerfilRepository, sedeRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsEventoRecordatorio3Dias() {
        assertEquals("EVENTO_RECORDATORIO_3DIAS", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConDatosFrescosDelEvento() {
        when(eventoRepository.findById(60L)).thenReturn(Optional.of(EventoPrivado.builder()
                .id(60L).idCliente(9L).idSede(2L)
                .fechaEvento(LocalDate.of(2026, 10, 5))
                .build()));
        when(clientePerfilRepository.buscarPorId(9L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(9L).nombres("Marco").correo("marco@correo.com").build()));
        when(sedeRepository.findById(2L)).thenReturn(Optional.of(Sede.builder().id(2L).nombre("Sede Sur").build()));

        Notificacion notificacion = Notificacion.builder().entidadId(60L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("marco@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Sede Sur"));
        assertTrue(resultado.getCuerpoHtml().contains("2026-10-05"));
    }
}
