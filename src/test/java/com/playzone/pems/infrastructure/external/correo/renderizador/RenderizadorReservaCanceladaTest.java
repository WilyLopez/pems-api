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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorReservaCanceladaTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;

    private RenderizadorReservaCancelada renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaCancelada(consultarReservasUseCase, new TemplateService());
    }

    @Test
    void testTipoCodigoEsReservaCancelada() {
        assertEquals("RESERVA_CANCELADA", renderizador.tipoCodigo());
    }

    @Test
    void testUsaMotivoCancelacionPersistidoEnLaReserva() {
        when(consultarReservasUseCase.consultarPorId(400L)).thenReturn(ReservaPublicaQuery.builder()
                .id(400L)
                .nombreCliente("<b onmouseover=alert(1)>Cliente</b>")
                .correoCliente("cliente@correo.com")
                .numeroTicket("TCK-0005")
                .fechaEvento(LocalDate.of(2026, 8, 15))
                .motivoCancelacion("Cliente solicitó reembolso")
                .build());

        Notificacion notificacion = Notificacion.builder().entidadId(400L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertTrue(resultado.getCuerpoHtml().contains("Cliente solicit"));
        assertFalse(resultado.getCuerpoHtml().contains("<b onmouseover=alert(1)>"));
        assertEquals("cliente@correo.com", resultado.getDestinatario());
    }
}
