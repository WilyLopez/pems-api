package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.port.in.ConsultarReservasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
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
class RenderizadorReservaReprogramadaTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;
    @Mock private SedeRepository sedeRepository;
    @Mock private TicketIngresoPdfService ticketIngresoPdfService;

    private RenderizadorReservaReprogramada renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaReprogramada(
                consultarReservasUseCase, sedeRepository, ticketIngresoPdfService,
                new TemplateService(), new ObjectMapper());
    }

    @Test
    void testTipoCodigoEsReservaReprogramada() {
        assertEquals("RESERVA_REPROGRAMADA", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConFechaAnteriorDesdeMetadataYFechaNuevaFresca() {
        when(consultarReservasUseCase.consultarPorId(700L)).thenReturn(ReservaPublicaQuery.builder()
                .id(700L)
                .nombreCliente("Elsa")
                .correoCliente("elsa@correo.com")
                .nombreSede("Sede Norte")
                .numeroTicket("TCK-0700")
                .fechaEvento(LocalDate.of(2026, 11, 20))
                .build());

        Notificacion notificacion = Notificacion.builder()
                .entidadId(700L)
                .metadata("{\"fechaAnterior\":\"2026-11-10\"}")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("elsa@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("2026-11-10"));
        assertTrue(resultado.getCuerpoHtml().contains("2026-11-20"));
    }
}
