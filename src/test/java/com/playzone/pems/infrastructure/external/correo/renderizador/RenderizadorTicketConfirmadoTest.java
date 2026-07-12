package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorTicketConfirmadoTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;
    @Mock private SedeRepository sedeRepository;
    @Mock private TicketIngresoPdfService ticketIngresoPdfService;

    private RenderizadorTicketConfirmado renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorTicketConfirmado(
                consultarReservasUseCase, sedeRepository, ticketIngresoPdfService, new TemplateService());
    }

    @Test
    void testTipoCodigoEsPagoConfirmado() {
        assertEquals("PAGO_CONFIRMADO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConPdfAdjunto() {
        ReservaPublicaQuery reserva = ReservaPublicaQuery.builder()
                .id(200L)
                .idSede(1L)
                .nombreCliente("Juan Perez")
                .correoCliente("juan@correo.com")
                .numeroTicket("TCK-0002")
                .fechaEvento(LocalDate.of(2026, 8, 5))
                .totalPagado(new BigDecimal("150.00"))
                .build();
        when(consultarReservasUseCase.consultarPorId(200L)).thenReturn(reserva);
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Central").build()));
        when(ticketIngresoPdfService.generarTicketPdf(any(), anyString())).thenReturn(new byte[]{1, 2, 3});

        Notificacion notificacion = Notificacion.builder().entidadId(200L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("juan@correo.com", resultado.getDestinatario());
        assertEquals(1, resultado.getAdjuntos().size());
        assertEquals("Ticket-TCK-0002.pdf", resultado.getAdjuntos().get(0).getNombreArchivo());
        assertArrayEquals(new byte[]{1, 2, 3}, resultado.getAdjuntos().get(0).getContenido());
        assertTrue(resultado.getCuerpoHtml().contains("TCK-0002"));
    }
}
