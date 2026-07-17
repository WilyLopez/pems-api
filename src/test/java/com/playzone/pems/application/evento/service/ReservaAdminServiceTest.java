package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.fidelizacion.port.in.RegistrarVisitaUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.util.FechaUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaAdminServiceTest {

    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private BloqueCalendarioRepository bloqueRepository;
    @Mock private ConfiguracionCalendarioRepository configRepository;
    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private RegistrarVisitaUseCase registrarVisitaUseCase;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private SedeScopeValidator sedeScope;
    @Mock private CrearNotificacionPort crearNotificacionPort;

    private ReservaAdminService service;

    @BeforeEach
    void setUp() {
        service = new ReservaAdminService(
                reservaRepository, clientePerfilRepository, feriadoRepository, bloqueRepository,
                configRepository, eventoRepository, registrarVisitaUseCase, ventaPagoRepository,
                authFacade, auditoria, sedeScope, crearNotificacionPort, new ObjectMapper());
    }

    private ReservaPublica reservaConfirmada() {
        return ReservaPublica.builder()
                .id(1L).idCliente(5L).idSede(1L)
                .estado(EstadoReservaPublica.CONFIRMADA)
                .canalReserva(CanalReserva.WEB)
                .fechaEvento(LocalDate.now().plusDays(1))
                .numeroTicket("TCK-0020")
                .ventaId(50L)
                .build();
    }

    @Test
    void testConfirmarIngresoNotificaInAppAlCliente() {
        ReservaPublica reserva = reservaConfirmada().toBuilder().fechaEvento(LocalDate.now()).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar(1L, UUID.randomUUID());

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("RESERVA_INGRESO_CONFIRMADO", captor.getValue().getTipoCodigo());
        assertEquals(5L, captor.getValue().getDestinatarioClienteId());
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    @Test
    void testConfirmarIngresoRechazaReservaPendiente() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .fechaEvento(LocalDate.now())
                .estado(EstadoReservaPublica.PENDIENTE)
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.ejecutar(1L, UUID.randomUUID()));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testConfirmarIngresoRechazaFechaFutura() {
        ReservaPublica reserva = reservaConfirmada().toBuilder().fechaEvento(LocalDate.now().plusDays(2)).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.ejecutar(1L, UUID.randomUUID()));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testMarcarEntradaRegistraAuditoriaYNotifica() {
        ReservaPublica reserva = reservaConfirmada().toBuilder().fechaEvento(LocalDate.now()).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .horaCierre(LocalTime.of(20, 0)).build());

        service.marcarEntrada(1L);

        verify(auditoria).ejecutar(any());

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("RESERVA_INGRESO_CONFIRMADO", captor.getValue().getTipoCodigo());
    }

    @Test
    void testMarcarEntradaCalculaPermanenciaFinAtSegunDuracionTarifa() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .fechaEvento(LocalDate.now())
                .duracionHistoricaMinutos(120)
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .horaCierre(LocalTime.of(23, 59)).build());

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        service.marcarEntrada(1L);
        verify(reservaRepository).save(captor.capture());

        ReservaPublica guardada = captor.getValue();
        assertEquals(guardada.getIngresoAt().plusMinutes(120), guardada.getPermanenciaFinAt());
    }

    @Test
    void testMarcarEntradaTopaPermanenciaEnHoraDeCierre() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .fechaEvento(LocalDate.now())
                .duracionHistoricaMinutos(120)
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .horaCierre(LocalTime.of(0, 1)).build());

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        service.marcarEntrada(1L);
        verify(reservaRepository).save(captor.capture());

        ReservaPublica guardada = captor.getValue();
        assertEquals(
                guardada.getFechaEvento().atTime(LocalTime.of(0, 1)).atZone(FechaUtil.ZONA_PERU).toOffsetDateTime(),
                guardada.getPermanenciaFinAt());
    }

    @Test
    void testBuscarTicketDetalleReportaPermanenciaVigente() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .fechaEvento(LocalDate.now())
                .estado(EstadoReservaPublica.COMPLETADA)
                .ingresado(true)
                .ingresoAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).minusMinutes(30))
                .permanenciaFinAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).plusMinutes(30))
                .build();
        when(reservaRepository.findByNumeroTicket("TCK-0020")).thenReturn(Optional.of(reserva));

        var detalle = service.buscarTicketDetalle("TCK-0020");

        assertTrue(detalle.isPermanenciaVigente());
    }

    @Test
    void testBuscarTicketDetalleReportaPermanenciaVencida() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .fechaEvento(LocalDate.now())
                .estado(EstadoReservaPublica.COMPLETADA)
                .ingresado(true)
                .ingresoAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).minusMinutes(150))
                .permanenciaFinAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).minusMinutes(30))
                .build();
        when(reservaRepository.findByNumeroTicket("TCK-0020")).thenReturn(Optional.of(reserva));

        var detalle = service.buscarTicketDetalle("TCK-0020");

        assertFalse(detalle.isPermanenciaVigente());
    }

    @Test
    void testRevertirIngresoLimpiaEstadoDeVisita() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .estado(EstadoReservaPublica.COMPLETADA)
                .ingresado(true)
                .ingresoAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU))
                .permanenciaFinAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).plusHours(2))
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var detalle = service.revertirIngreso(1L);

        assertFalse(detalle.isYaIngreso());
        assertEquals("CONFIRMADA", detalle.getEstado());
    }

    @Test
    void testRevertirIngresoRechazaSiNoHayIngresoRegistrado() {
        ReservaPublica reserva = reservaConfirmada().toBuilder().ingresado(false).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.revertirIngreso(1L));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testRegistrarSalidaExigeIngresoPrevio() {
        ReservaPublica reserva = reservaConfirmada().toBuilder().ingresado(false).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.registrarSalida(1L));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testRegistrarSalidaEstableceSalidaRealAt() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .estado(EstadoReservaPublica.COMPLETADA)
                .ingresado(true)
                .ingresoAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).minusMinutes(30))
                .permanenciaFinAt(java.time.OffsetDateTime.now(FechaUtil.ZONA_PERU).plusMinutes(90))
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        service.registrarSalida(1L);
        verify(reservaRepository).save(captor.capture());

        assertNotNull(captor.getValue().getSalidaRealAt());
    }

    @Test
    void testMarcarEntradaRechazaTicketDeFechaFutura() {
        ReservaPublica reserva = reservaConfirmada().toBuilder().fechaEvento(LocalDate.now().plusDays(3)).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.marcarEntrada(1L));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testMarcarEntradaRechazaTicketReprogramado() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .fechaEvento(LocalDate.now())
                .estado(EstadoReservaPublica.REPROGRAMADA)
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.marcarEntrada(1L));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testEditarFechaRechazaTicketReprogramado() {
        ReservaPublica reserva = reservaConfirmada().toBuilder()
                .estado(EstadoReservaPublica.REPROGRAMADA)
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(ValidationException.class, () -> service.editarFecha(1L, LocalDate.now().plusDays(10)));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testEditarFechaNotificaTransaccionalReservaReprogramada() {
        ReservaPublica reserva = reservaConfirmada();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .diasMaxReservaPublica(90).aforoMaximo(100).build());
        when(feriadoRepository.existsByFecha(any())).thenReturn(false);
        when(bloqueRepository.existsBloqueActivoEnFecha(anyLong(), any())).thenReturn(false);
        when(eventoRepository.existsActivoBySedeAndFecha(anyLong(), any())).thenReturn(false);
        when(reservaRepository.countActivasBySedeAndFecha(anyLong(), any())).thenReturn(0);

        LocalDate nuevaFecha = LocalDate.now().plusDays(10);
        service.editarFecha(1L, nuevaFecha);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("RESERVA_REPROGRAMADA", cmd.getTipoCodigo());
        assertEquals(5L, cmd.getDestinatarioClienteId());
        assertEquals(nuevaFecha.toString(), cmd.getDatosExtra().get("fechaNueva"));
        assertTrue(cmd.getMetadata().contains("fechaAnterior"));
    }
}
