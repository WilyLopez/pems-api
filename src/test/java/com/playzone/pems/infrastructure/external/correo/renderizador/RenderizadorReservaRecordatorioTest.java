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
class RenderizadorReservaRecordatorioTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;

    private RenderizadorReservaRecordatorio renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaRecordatorio(consultarReservasUseCase, new TemplateService());
    }

    @Test
    void testTipoCodigoEsReservaRecordatorio() {
        assertEquals("RESERVA_RECORDATORIO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConDatosFrescosDeLaReserva() {
        when(consultarReservasUseCase.consultarPorId(500L)).thenReturn(ReservaPublicaQuery.builder()
                .id(500L)
                .nombreCliente("Rosa")
                .correoCliente("rosa@correo.com")
                .nombreSede("Sede Norte")
                .numeroTicket("TCK-0500")
                .fechaEvento(LocalDate.of(2026, 9, 1))
                .build());

        Notificacion notificacion = Notificacion.builder().entidadId(500L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("rosa@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("Sede Norte"));
        assertTrue(resultado.getCuerpoHtml().contains("TCK-0500"));
    }
}
