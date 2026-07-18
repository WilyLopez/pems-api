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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorReservaPendienteYapeTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;

    private RenderizadorReservaPendienteYape renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaPendienteYape(consultarReservasUseCase, new TemplateService());
        ReflectionTestUtils.setField(renderizador, "frontendUrl", "https://kikiylala.lat");
    }

    @Test
    void testTipoCodigoEsReservaPendienteYape() {
        assertEquals("RESERVA_PENDIENTE_YAPE", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConEnlaceALaReservaComoQueryParam() {
        when(consultarReservasUseCase.consultarPorId(400L)).thenReturn(ReservaPublicaQuery.builder()
                .id(400L)
                .nombreCliente("Luis Vega")
                .correoCliente("luis@correo.com")
                .numeroTicket("TCK-0005")
                .fechaEvento(LocalDate.of(2026, 9, 1))
                .totalPagado(new BigDecimal("120.00"))
                .build());

        Notificacion notificacion = Notificacion.builder().entidadId(400L).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("luis@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("https://kikiylala.lat/cliente/mis-reservas?detalle=400"));
    }
}
