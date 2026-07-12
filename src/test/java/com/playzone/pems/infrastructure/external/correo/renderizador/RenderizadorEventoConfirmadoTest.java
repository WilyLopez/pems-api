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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorEventoConfirmadoTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private TurnoRepository turnoRepository;

    private RenderizadorEventoConfirmado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorEventoConfirmado(
                eventoRepository, clientePerfilRepository, turnoRepository, new TemplateService());
    }

    @Test
    void testTipoCodigoEsEventoConfirmado() {
        assertEquals("EVENTO_CONFIRMADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaMuestraMontosFrescosDelEvento() {
        when(eventoRepository.findById(60L)).thenReturn(Optional.of(EventoPrivado.builder()
                .id(60L).idCliente(8L).idTurno(4L)
                .fechaEvento(LocalDate.of(2026, 10, 1))
                .tipoEvento("Quince anos")
                .aforoDeclarado(50)
                .precioContrato(new BigDecimal("1000.00"))
                .montoAdelanto(new BigDecimal("300.00"))
                .build()));
        when(clientePerfilRepository.buscarPorId(8L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(8L).nombres("Luis").correo("luis@correo.com").build()));
        when(turnoRepository.findById(4L)).thenReturn(Optional.of(Turno.builder()
                .id(4L).descripcion("Noche").horaInicio(LocalTime.of(19, 0)).horaFin(LocalTime.of(23, 0)).build()));

        Notificacion notificacion = Notificacion.builder().entidadId(60L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("luis@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("300.00"));
        assertTrue(resultado.getCuerpoHtml().contains("700.00"));
    }
}
