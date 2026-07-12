package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.notificacion.model.NotificacionEntrega;
import com.playzone.pems.domain.notificacion.repository.NotificacionEntregaRepository;
import com.playzone.pems.domain.notificacion.repository.NotificacionRepository;
import com.playzone.pems.infrastructure.external.correo.JavaMailCorreoClient;
import com.playzone.pems.infrastructure.external.correo.renderizador.AdjuntoCorreo;
import com.playzone.pems.infrastructure.external.correo.renderizador.ContenidoCorreo;
import com.playzone.pems.infrastructure.external.correo.renderizador.RenderizadorCorreoRegistry;
import com.playzone.pems.infrastructure.external.correo.renderizador.RenderizadorCorreoTransaccional;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionEntregaEmailSchedulerTest {

    @Mock private NotificacionEntregaRepository entregaRepo;
    @Mock private NotificacionRepository notificacionRepo;
    @Mock private RenderizadorCorreoRegistry renderizadores;
    @Mock private JavaMailCorreoClient correoClient;
    @Mock private CrearNotificacionPort notificacionPort;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;
    @Mock private RenderizadorCorreoTransaccional renderizador;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private NotificacionEntregaEmailScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NotificacionEntregaEmailScheduler(
                entregaRepo, notificacionRepo, renderizadores, correoClient, notificacionPort,
                resolverAdministradoresPort, meterRegistry);
    }

    private NotificacionEntrega entregaPendiente() {
        return NotificacionEntrega.builder()
                .id(1L).notificacionId(10L).canal("EMAIL").estado("PENDIENTE").intentos(0).build();
    }

    private Notificacion notificacionPagoConfirmado() {
        return Notificacion.builder().id(10L).tipoCodigo("PAGO_CONFIRMADO").entidadId(100L).build();
    }

    @Test
    void testEnvioExitosoMarcaEnviado() {
        NotificacionEntrega entrega = entregaPendiente();
        when(entregaRepo.findPendientesEmail(anyInt())).thenReturn(List.of(entrega));
        when(notificacionRepo.findById(10L)).thenReturn(Optional.of(notificacionPagoConfirmado()));
        when(renderizadores.resolver("PAGO_CONFIRMADO")).thenReturn(Optional.of(renderizador));
        when(renderizador.renderizar(any())).thenReturn(ContenidoCorreo.builder()
                .destinatario("cliente@correo.com").asunto("Asunto").cuerpoHtml("<p>Hola</p>").build());

        scheduler.procesarPendientes();

        verify(correoClient).enviar("cliente@correo.com", "Asunto", "<p>Hola</p>");
        ArgumentCaptor<NotificacionEntrega> captor = ArgumentCaptor.forClass(NotificacionEntrega.class);
        verify(entregaRepo).save(captor.capture());
        assertEquals("ENVIADO", captor.getValue().getEstado());
        assertNotNull(captor.getValue().getEnviadoAt());
        assertEquals(1.0, meterRegistry.counter("notificacion.email.enviados", "tipo", "PAGO_CONFIRMADO").count());
        assertNotNull(meterRegistry.find("notificacion.email.lote.duracion").timer());
    }

    @Test
    void testEnvioConAdjuntosUsaMetodoCorrespondiente() {
        NotificacionEntrega entrega = entregaPendiente();
        when(entregaRepo.findPendientesEmail(anyInt())).thenReturn(List.of(entrega));
        when(notificacionRepo.findById(10L)).thenReturn(Optional.of(notificacionPagoConfirmado()));
        when(renderizadores.resolver("PAGO_CONFIRMADO")).thenReturn(Optional.of(renderizador));
        when(renderizador.renderizar(any())).thenReturn(ContenidoCorreo.builder()
                .destinatario("cliente@correo.com").asunto("Asunto").cuerpoHtml("<p>Hola</p>")
                .adjuntos(List.of(AdjuntoCorreo.builder().nombreArchivo("a.pdf").contenido(new byte[]{1}).tipoContenido("application/pdf").build()))
                .build());

        scheduler.procesarPendientes();

        verify(correoClient).enviarConAdjuntos(eq("cliente@correo.com"), eq("Asunto"), eq("<p>Hola</p>"), anyList());
        verify(correoClient, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void testFalloTransitorioMarcaErrorSinAlertarAdmins() {
        NotificacionEntrega entrega = entregaPendiente();
        when(entregaRepo.findPendientesEmail(anyInt())).thenReturn(List.of(entrega));
        when(notificacionRepo.findById(10L)).thenReturn(Optional.of(notificacionPagoConfirmado()));
        when(renderizadores.resolver("PAGO_CONFIRMADO")).thenReturn(Optional.of(renderizador));
        when(renderizador.renderizar(any())).thenReturn(ContenidoCorreo.builder()
                .destinatario("cliente@correo.com").asunto("Asunto").cuerpoHtml("<p>Hola</p>").build());
        doThrow(new RuntimeException("smtp caido")).when(correoClient).enviar(anyString(), anyString(), anyString());

        scheduler.procesarPendientes();

        ArgumentCaptor<NotificacionEntrega> captor = ArgumentCaptor.forClass(NotificacionEntrega.class);
        verify(entregaRepo).save(captor.capture());
        assertEquals("ERROR", captor.getValue().getEstado());
        assertEquals(1, captor.getValue().getIntentos());
        verify(resolverAdministradoresPort, never()).obtenerIdsAdministradoresActivos();
        assertEquals(1.0, meterRegistry.counter("notificacion.email.fallidos", "tipo", "PAGO_CONFIRMADO").count());
    }

    @Test
    void testFalloAgotadoMarcaRebotadoYAlertaAdmins() {
        NotificacionEntrega entrega = NotificacionEntrega.builder()
                .id(1L).notificacionId(10L).canal("EMAIL").estado("ERROR").intentos(2).build();
        when(entregaRepo.findPendientesEmail(anyInt())).thenReturn(List.of(entrega));
        when(notificacionRepo.findById(10L)).thenReturn(Optional.of(notificacionPagoConfirmado()));
        when(renderizadores.resolver("PAGO_CONFIRMADO")).thenReturn(Optional.of(renderizador));
        when(renderizador.renderizar(any())).thenReturn(ContenidoCorreo.builder()
                .destinatario("cliente@correo.com").asunto("Asunto").cuerpoHtml("<p>Hola</p>").build());
        doThrow(new RuntimeException("smtp caido")).when(correoClient).enviar(anyString(), anyString(), anyString());

        UUID idAdmin = UUID.randomUUID();
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(idAdmin));

        scheduler.procesarPendientes();

        ArgumentCaptor<NotificacionEntrega> captor = ArgumentCaptor.forClass(NotificacionEntrega.class);
        verify(entregaRepo).save(captor.capture());
        assertEquals("REBOTADO", captor.getValue().getEstado());
        assertEquals(3, captor.getValue().getIntentos());

        ArgumentCaptor<com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand> cmdCaptor =
                ArgumentCaptor.forClass(com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand.class);
        verify(notificacionPort).notificar(cmdCaptor.capture());
        assertEquals("ERROR_ENVIO_EMAIL", cmdCaptor.getValue().getTipoCodigo());
        assertEquals(idAdmin, cmdCaptor.getValue().getDestinatarioUsuarioId());
    }

    @Test
    void testSinRenderizadorMarcaRebotadoInmediatamente() {
        NotificacionEntrega entrega = entregaPendiente();
        when(entregaRepo.findPendientesEmail(anyInt())).thenReturn(List.of(entrega));
        when(notificacionRepo.findById(10L)).thenReturn(Optional.of(notificacionPagoConfirmado()));
        when(renderizadores.resolver("PAGO_CONFIRMADO")).thenReturn(Optional.empty());
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        scheduler.procesarPendientes();

        ArgumentCaptor<NotificacionEntrega> captor = ArgumentCaptor.forClass(NotificacionEntrega.class);
        verify(entregaRepo).save(captor.capture());
        assertEquals("REBOTADO", captor.getValue().getEstado());
        verify(correoClient, never()).enviar(anyString(), anyString(), anyString());
    }

    @Test
    void testDestinatarioVacioMarcaRebotadoInmediatamente() {
        NotificacionEntrega entrega = entregaPendiente();
        when(entregaRepo.findPendientesEmail(anyInt())).thenReturn(List.of(entrega));
        when(notificacionRepo.findById(10L)).thenReturn(Optional.of(notificacionPagoConfirmado()));
        when(renderizadores.resolver("PAGO_CONFIRMADO")).thenReturn(Optional.of(renderizador));
        when(renderizador.renderizar(any())).thenReturn(ContenidoCorreo.builder()
                .destinatario(null).asunto("Asunto").cuerpoHtml("<p>Hola</p>").build());
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of());

        scheduler.procesarPendientes();

        ArgumentCaptor<NotificacionEntrega> captor = ArgumentCaptor.forClass(NotificacionEntrega.class);
        verify(entregaRepo).save(captor.capture());
        assertEquals("REBOTADO", captor.getValue().getEstado());
        verify(correoClient, never()).enviar(anyString(), anyString(), anyString());
    }
}
