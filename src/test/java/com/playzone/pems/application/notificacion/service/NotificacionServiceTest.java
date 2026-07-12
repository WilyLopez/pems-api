package com.playzone.pems.application.notificacion.service;

import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.dto.query.EstadoEntregaQuery;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.notificacion.model.NotificacionEntrega;
import com.playzone.pems.domain.notificacion.model.TipoNotificacion;
import com.playzone.pems.domain.notificacion.repository.NotificacionEntregaRepository;
import com.playzone.pems.domain.notificacion.repository.NotificacionRepository;
import com.playzone.pems.domain.notificacion.repository.TipoNotificacionRepository;
import com.playzone.pems.domain.preferencia.model.PreferenciaUsuario;
import com.playzone.pems.domain.preferencia.repository.PreferenciaUsuarioRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceTest {

    @Mock private TipoNotificacionRepository tipoRepo;
    @Mock private NotificacionRepository notifRepo;
    @Mock private NotificacionEntregaRepository entregaRepo;
    @Mock private PreferenciaUsuarioRepository preferenciaUsuarioRepo;

    private NotificacionService service;

    @BeforeEach
    void setUp() {
        service = new NotificacionService(tipoRepo, notifRepo, entregaRepo, preferenciaUsuarioRepo);
    }

    private TipoNotificacion tipoConCanales(List<String> canales) {
        return tipoConCanales(canales, false);
    }

    private TipoNotificacion tipoConCanales(List<String> canales, boolean esObligatoria) {
        return TipoNotificacion.builder()
                .codigo("PAGO_CONFIRMADO")
                .plantillaTitulo("Pago confirmado")
                .plantillaMensaje("Tu pago de S/ {monto} fue confirmado")
                .prioridad("NORMAL")
                .canalesDefault(canales)
                .esObligatoria(esObligatoria)
                .build();
    }

    @Test
    void testNotificarTransaccionalPersisteSincronoYCreaEntregaEmail() {
        when(tipoRepo.findByCodigo("PAGO_CONFIRMADO")).thenReturn(Optional.of(tipoConCanales(List.of("IN_APP", "EMAIL"))));
        when(notifRepo.save(any())).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(1L);
            return n;
        });

        CrearNotificacionCommand cmd = CrearNotificacionCommand.builder()
                .tipoCodigo("PAGO_CONFIRMADO")
                .destinatarioClienteId(42L)
                .entidadTipo("reserva_publica")
                .entidadId(100L)
                .datosExtra(Map.of("monto", "50.00"))
                .build();

        service.notificarTransaccional(cmd);

        verify(notifRepo).save(any());
        ArgumentCaptor<List<com.playzone.pems.domain.notificacion.model.NotificacionEntrega>> captor = ArgumentCaptor.forClass(List.class);
        verify(entregaRepo).saveAll(captor.capture());

        List<String> canales = captor.getValue().stream()
                .map(com.playzone.pems.domain.notificacion.model.NotificacionEntrega::getCanal)
                .toList();
        assertTrue(canales.contains("IN_APP"));
        assertTrue(canales.contains("EMAIL"));
    }

    @Test
    void testNotificarTransaccionalPropagaExcepcionSiTipoNoExiste() {
        when(tipoRepo.findByCodigo("TIPO_INEXISTENTE")).thenReturn(Optional.empty());

        CrearNotificacionCommand cmd = CrearNotificacionCommand.builder()
                .tipoCodigo("TIPO_INEXISTENTE")
                .destinatarioClienteId(1L)
                .build();

        assertThrows(ResourceNotFoundException.class, () -> service.notificarTransaccional(cmd));
        verify(notifRepo, never()).save(any());
    }

    @Test
    void testNotificarNoPropagaExcepcion() {
        when(tipoRepo.findByCodigo("TIPO_INEXISTENTE")).thenReturn(Optional.empty());

        CrearNotificacionCommand cmd = CrearNotificacionCommand.builder()
                .tipoCodigo("TIPO_INEXISTENTE")
                .destinatarioUsuarioId(UUID.randomUUID())
                .build();

        assertDoesNotThrow(() -> service.notificar(cmd));
    }

    @Test
    void testStaffConNotificacionesEmailDesactivadasNoRecibeEntregaEmailSiNoEsObligatoria() {
        UUID staffId = UUID.randomUUID();
        when(tipoRepo.findByCodigo("PAGO_CONFIRMADO")).thenReturn(Optional.of(
                tipoConCanales(List.of("IN_APP", "EMAIL"), false)));
        when(notifRepo.save(any())).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(2L);
            return n;
        });
        when(preferenciaUsuarioRepo.buscarPorUsuarioId(staffId)).thenReturn(Optional.of(
                PreferenciaUsuario.builder().usuarioId(staffId).notificacionesEmail(false).build()));

        service.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("PAGO_CONFIRMADO")
                .destinatarioUsuarioId(staffId)
                .build());

        ArgumentCaptor<List<NotificacionEntrega>> captor = ArgumentCaptor.forClass(List.class);
        verify(entregaRepo).saveAll(captor.capture());
        List<String> canales = captor.getValue().stream().map(NotificacionEntrega::getCanal).toList();
        assertTrue(canales.contains("IN_APP"));
        assertFalse(canales.contains("EMAIL"));
    }

    @Test
    void testStaffConNotificacionesEmailDesactivadasSiRecibeEntregaEmailSiEsObligatoria() {
        UUID staffId = UUID.randomUUID();
        when(tipoRepo.findByCodigo("PAGO_CONFIRMADO")).thenReturn(Optional.of(
                tipoConCanales(List.of("IN_APP", "EMAIL"), true)));
        when(notifRepo.save(any())).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(3L);
            return n;
        });

        service.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("PAGO_CONFIRMADO")
                .destinatarioUsuarioId(staffId)
                .build());

        ArgumentCaptor<List<NotificacionEntrega>> captor = ArgumentCaptor.forClass(List.class);
        verify(entregaRepo).saveAll(captor.capture());
        List<String> canales = captor.getValue().stream().map(NotificacionEntrega::getCanal).toList();
        assertTrue(canales.contains("EMAIL"));
        verify(preferenciaUsuarioRepo, never()).buscarPorUsuarioId(any());
    }

    @Test
    void testClienteSinMecanismoDePreferenciaSiempreRecibeEmailAunNoObligatoria() {
        when(tipoRepo.findByCodigo("PAGO_CONFIRMADO")).thenReturn(Optional.of(
                tipoConCanales(List.of("IN_APP", "EMAIL"), false)));
        when(notifRepo.save(any())).thenAnswer(inv -> {
            Notificacion n = inv.getArgument(0);
            n.setId(4L);
            return n;
        });

        service.notificarTransaccional(CrearNotificacionCommand.builder()
                .tipoCodigo("PAGO_CONFIRMADO")
                .destinatarioClienteId(7L)
                .build());

        ArgumentCaptor<List<NotificacionEntrega>> captor = ArgumentCaptor.forClass(List.class);
        verify(entregaRepo).saveAll(captor.capture());
        List<String> canales = captor.getValue().stream().map(NotificacionEntrega::getCanal).toList();
        assertTrue(canales.contains("EMAIL"));
        verify(preferenciaUsuarioRepo, never()).buscarPorUsuarioId(any());
    }

    @Test
    void testConsultarPorEntidadRetornaEnviadoCuandoLaEntregaEmailFueExitosa() {
        Notificacion notif = Notificacion.builder()
                .id(10L).entidadTipo("reserva_publica").entidadId(100L).build();
        when(notifRepo.findUltimaPorEntidad("reserva_publica", 100L)).thenReturn(Optional.of(notif));
        when(entregaRepo.findByNotificacionId(10L)).thenReturn(List.of(
                NotificacionEntrega.builder().notificacionId(10L).canal("IN_APP").estado("ENVIADO").build(),
                NotificacionEntrega.builder().notificacionId(10L).canal("EMAIL").estado("ENVIADO").build()));

        EstadoEntregaQuery resultado = service.consultarPorEntidad("reserva_publica", 100L);

        assertEquals("ENVIADO", resultado.getEstado());
    }

    @Test
    void testConsultarPorEntidadRetornaErrorCuandoLaEntregaEmailFueRebotada() {
        Notificacion notif = Notificacion.builder()
                .id(11L).entidadTipo("reserva_publica").entidadId(101L).build();
        when(notifRepo.findUltimaPorEntidad("reserva_publica", 101L)).thenReturn(Optional.of(notif));
        when(entregaRepo.findByNotificacionId(11L)).thenReturn(List.of(
                NotificacionEntrega.builder().notificacionId(11L).canal("EMAIL").estado("REBOTADO")
                        .mensajeError("Buzón lleno").build()));

        EstadoEntregaQuery resultado = service.consultarPorEntidad("reserva_publica", 101L);

        assertEquals("ERROR", resultado.getEstado());
        assertEquals("Buzón lleno", resultado.getMensajeError());
    }

    @Test
    void testConsultarPorEntidadRetornaPendienteCuandoLaEntregaEmailAunNoSeProcesa() {
        Notificacion notif = Notificacion.builder()
                .id(12L).entidadTipo("reserva_publica").entidadId(102L).build();
        when(notifRepo.findUltimaPorEntidad("reserva_publica", 102L)).thenReturn(Optional.of(notif));
        when(entregaRepo.findByNotificacionId(12L)).thenReturn(List.of(
                NotificacionEntrega.builder().notificacionId(12L).canal("EMAIL").estado("PENDIENTE").build()));

        EstadoEntregaQuery resultado = service.consultarPorEntidad("reserva_publica", 102L);

        assertEquals("PENDIENTE", resultado.getEstado());
    }

    @Test
    void testConsultarPorEntidadRetornaSinEnvioCuandoNoHayNotificacion() {
        when(notifRepo.findUltimaPorEntidad("reserva_publica", 999L)).thenReturn(Optional.empty());

        EstadoEntregaQuery resultado = service.consultarPorEntidad("reserva_publica", 999L);

        assertEquals("SIN_ENVIO", resultado.getEstado());
    }

    @Test
    void testConsultarPorEntidadRetornaSinEnvioCuandoLaNotificacionNoTieneCanalEmail() {
        Notificacion notif = Notificacion.builder()
                .id(13L).entidadTipo("reserva_publica").entidadId(103L).build();
        when(notifRepo.findUltimaPorEntidad("reserva_publica", 103L)).thenReturn(Optional.of(notif));
        when(entregaRepo.findByNotificacionId(13L)).thenReturn(List.of(
                NotificacionEntrega.builder().notificacionId(13L).canal("IN_APP").estado("ENVIADO").build()));

        EstadoEntregaQuery resultado = service.consultarPorEntidad("reserva_publica", 103L);

        assertEquals("SIN_ENVIO", resultado.getEstado());
    }
}
