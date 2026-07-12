package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorAbonoEventoTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;

    private RenderizadorAbonoEvento renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorAbonoEvento(
                eventoRepository, clientePerfilRepository, new TemplateService(), new ObjectMapper());
    }

    @Test
    void testTipoCodigoEsEventoAbonoRecibido() {
        assertEquals("EVENTO_ABONO_RECIBIDO", renderizador.tipoCodigo());
    }

    @Test
    void testUsaMontoAbonadoDeMetadataYSaldoFrescoDelEvento() {
        when(eventoRepository.findById(70L)).thenReturn(Optional.of(EventoPrivado.builder()
                .id(70L).idCliente(9L)
                .fechaEvento(LocalDate.of(2026, 11, 5))
                .precioContrato(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .build()));
        when(clientePerfilRepository.buscarPorId(9L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(9L).nombres("Rosa").correo("rosa@correo.com").build()));

        Notificacion notificacion = Notificacion.builder()
                .entidadId(70L)
                .metadata("{\"montoAbonado\":\"150.00\"}")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("rosa@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("150.00"));
        assertTrue(resultado.getCuerpoHtml().contains("300.00"));
    }

    @Test
    void testMetadataAusenteUsaMontoCero() {
        when(eventoRepository.findById(71L)).thenReturn(Optional.of(EventoPrivado.builder()
                .id(71L).idCliente(9L)
                .fechaEvento(LocalDate.of(2026, 11, 5))
                .precioContrato(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .build()));
        when(clientePerfilRepository.buscarPorId(9L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(9L).nombres("Rosa").correo("rosa@correo.com").build()));

        Notificacion notificacion = Notificacion.builder().entidadId(71L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertTrue(resultado.getCuerpoHtml().contains("0.00"));
    }
}
