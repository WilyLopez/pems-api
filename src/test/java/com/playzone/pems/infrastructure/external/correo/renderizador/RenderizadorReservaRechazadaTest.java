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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorReservaRechazadaTest {

    @Mock private ConsultarReservasUseCase consultarReservasUseCase;

    private RenderizadorReservaRechazada renderizador;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorReservaRechazada(consultarReservasUseCase, new TemplateService(), objectMapper);
        ReflectionTestUtils.setField(renderizador, "frontendUrl", "https://kikiylala.lat");
    }

    @Test
    void testTipoCodigoEsPagoRechazado() {
        assertEquals("PAGO_RECHAZADO", renderizador.tipoCodigo());
    }

    @Test
    void testExtraeMotivoDeLaMetadataYLoEscapa() throws Exception {
        when(consultarReservasUseCase.consultarPorId(300L)).thenReturn(ReservaPublicaQuery.builder()
                .id(300L)
                .nombreCliente("Juan Perez")
                .correoCliente("juan@correo.com")
                .numeroTicket("TCK-0003")
                .fechaEvento(LocalDate.of(2026, 8, 10))
                .build());

        String metadata = objectMapper.writeValueAsString(Map.of("motivo", "<script>alert(1)</script>"));
        Notificacion notificacion = Notificacion.builder().entidadId(300L).metadata(metadata).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertFalse(resultado.getCuerpoHtml().contains("<script>alert(1)</script>"));
        assertTrue(resultado.getCuerpoHtml().contains("&lt;script&gt;"));
        assertTrue(resultado.getCuerpoHtml().contains("https://kikiylala.lat/cliente/mis-reservas?detalle=300"));
    }

    @Test
    void testUsaMotivoPorDefectoSiMetadataEsNula() {
        when(consultarReservasUseCase.consultarPorId(301L)).thenReturn(ReservaPublicaQuery.builder()
                .id(301L)
                .nombreCliente("Ana Diaz")
                .correoCliente("ana@correo.com")
                .numeroTicket("TCK-0004")
                .fechaEvento(LocalDate.of(2026, 8, 11))
                .build());

        Notificacion notificacion = Notificacion.builder().entidadId(301L).metadata(null).build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertTrue(resultado.getCuerpoHtml().contains("Comprobante inv"));
    }
}
