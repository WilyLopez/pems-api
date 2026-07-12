package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorReservaPendienteTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;

    private RenderizadorReservaPendiente renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaPendiente(consultarReservasUseCase, new TemplateService());
    }

    @Test
    void testTipoCodigoEsTicketDisponible() {
        assertEquals("TICKET_DISPONIBLE", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConDatosFrescosDeLaReserva() {
        when(consultarReservasUseCase.consultarPorId(100L)).thenReturn(ReservaPublicaQuery.builder()
                .id(100L)
                .nombreCliente("<script>alert(1)</script>")
                .correoCliente("cliente@correo.com")
                .numeroTicket("TCK-0001")
                .fechaEvento(LocalDate.of(2026, 8, 1))
                .totalPagado(new BigDecimal("120.00"))
                .build());

        Notificacion notificacion = Notificacion.builder().entidadId(100L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("cliente@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("TCK-0001"));
        assertFalse(resultado.getCuerpoHtml().contains("<script>alert(1)</script>"));
        assertTrue(resultado.getCuerpoHtml().contains("&lt;script&gt;"));
        assertNull(resultado.getAdjuntos());
    }
}
