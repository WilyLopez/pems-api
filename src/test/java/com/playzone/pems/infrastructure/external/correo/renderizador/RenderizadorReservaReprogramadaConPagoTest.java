package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorReservaReprogramadaConPagoTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;

    private RenderizadorReservaReprogramadaConPago renderizador;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaReprogramadaConPago(
                consultarReservasUseCase, new TemplateService(), objectMapper);
        ReflectionTestUtils.setField(renderizador, "frontendUrl", "https://kikiylala.lat");
    }

    @Test
    void testTipoCodigoEsReservaReprogramadaConPago() {
        assertEquals("RESERVA_REPROGRAMADA_CON_PAGO", renderizador.tipoCodigo());
    }

    @Test
    void testRenderizaConEnlaceALaReservaComoQueryParam() throws Exception {
        when(consultarReservasUseCase.consultarPorId(500L)).thenReturn(ReservaPublicaQuery.builder()
                .id(500L)
                .nombreCliente("Carla Ruiz")
                .correoCliente("carla@correo.com")
                .numeroTicket("TCK-0006")
                .fechaEvento(LocalDate.of(2026, 9, 15))
                .build());

        String metadata = objectMapper.writeValueAsString(
                Map.of("fechaAnterior", "2026-09-01", "montoAdicional", "15.00"));
        Notificacion notificacion = Notificacion.builder().entidadId(500L).metadata(metadata).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("carla@correo.com", resultado.getDestinatario());
        assertTrue(resultado.getCuerpoHtml().contains("https://kikiylala.lat/cliente/mis-reservas?detalle=500"));
    }
}
