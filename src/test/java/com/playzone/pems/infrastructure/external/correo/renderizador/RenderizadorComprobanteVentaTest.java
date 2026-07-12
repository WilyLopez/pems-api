package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;
import com.playzone.pems.application.venta.port.in.ConsultarVentasUseCase;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.NotaVentaPdfService;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import com.playzone.pems.infrastructure.template.TemplateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RenderizadorComprobanteVentaTest {

    @Mock private ConsultarVentasUseCase consultarVentasUseCase;
    @Mock private SedeRepository sedeRepository;
    @Mock private NotaVentaPdfService notaVentaPdfService;
    @Mock private TicketIngresoPdfService ticketIngresoPdfService;

    private RenderizadorComprobanteVenta renderizador;

    @BeforeEach
    void setUp() {
        renderizador = new RenderizadorComprobanteVenta(
                consultarVentasUseCase, sedeRepository, notaVentaPdfService,
                ticketIngresoPdfService, new TemplateService(), new ObjectMapper());
    }

    @Test
    void testTipoCodigoEsDocumentoListo() {
        assertEquals("DOCUMENTO_LISTO", renderizador.tipoCodigo());
    }

    @Test
    void testUsaDestinatarioDeMetadataYAdjuntaNotaMasTickets() {
        VentaDetalleQuery detalle = VentaDetalleQuery.builder()
                .id(90L).idSede(1L).clienteId(12L)
                .nombreCliente("Pedro")
                .fechaVisita(LocalDate.of(2026, 8, 1))
                .total(new BigDecimal("120.00"))
                .tickets(List.of(ReservaPublicaQuery.builder().numeroTicket("TCK-0099").build()))
                .build();
        when(consultarVentasUseCase.consultarDetallePorId(90L)).thenReturn(detalle);
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));
        when(notaVentaPdfService.generarNotaVentaPdf(any(), eq("Sede Norte"))).thenReturn(new byte[]{1});
        when(ticketIngresoPdfService.generarTicketPdf(any(), eq("Sede Norte"))).thenReturn(new byte[]{2});

        Notificacion notificacion = Notificacion.builder()
                .entidadId(90L)
                .metadata("{\"destinatario\":\"otro@correo.com\"}")
                .build();

        ContenidoCorreo resultado = renderizador.renderizar(notificacion);

        assertEquals("otro@correo.com", resultado.getDestinatario());
        assertEquals(2, resultado.getAdjuntos().size());
        assertTrue(resultado.getAdjuntos().stream().anyMatch(a -> a.getNombreArchivo().equals("NotaVenta-90.pdf")));
        assertTrue(resultado.getAdjuntos().stream().anyMatch(a -> a.getNombreArchivo().equals("Ticket-TCK-0099.pdf")));
    }
}
