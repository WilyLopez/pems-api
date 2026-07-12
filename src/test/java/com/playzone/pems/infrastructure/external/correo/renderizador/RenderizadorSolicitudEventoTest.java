package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
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
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorSolicitudEventoTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private TurnoRepository turnoRepository;

    private RenderizadorSolicitudEvento renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorSolicitudEvento(
                eventoRepository, clientePerfilRepository, turnoRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsEventoPresupuestoEnviado() {
        assertEquals("EVENTO_PRESUPUESTO_ENVIADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConDatosFrescosDelEvento() {
        when(eventoRepository.findById(50L)).thenReturn(Optional.of(EventoPrivado.builder()
                .id(50L).idCliente(7L).idTurno(3L)
                .fechaEvento(LocalDate.of(2026, 9, 20))
                .tipoEvento("Cumpleanos")
                .aforoDeclarado(30)
                .build()));
        when(clientePerfilRepository.buscarPorId(7L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(7L).nombres("<b>Ana</b>").correo("ana@correo.com").build()));
        when(turnoRepository.findById(3L)).thenReturn(Optional.of(Turno.builder()
                .id(3L).descripcion("Tarde").horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(18, 0)).build()));

        Notificacion notificacion = Notificacion.builder().entidadId(50L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("ana@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Ana"));
        assertFalse(resultado.getCuerpoHtml().contains("<b>Ana</b>"));
        assertTrue(resultado.getCuerpoHtml().contains("Cumpleanos"));
    }
}
