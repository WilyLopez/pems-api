package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.command.CrearReservaPublicaCommand;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaPublicaServiceTest {

    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private TarifaRepository tarifaRepository;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private BloqueCalendarioRepository bloqueRepository;
    @Mock private ConfiguracionCalendarioRepository configRepository;
    @Mock private StoragePort storagePort;
    @Mock private VentaRepository ventaRepository;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private SupabaseAuthFacade supabaseAuthFacade;
    @Mock private ConfiguracionGlobalRepository configuracionGlobalRepository;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private SedeScopeValidator sedeScope;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;

    private ReservaPublicaService service;

    @BeforeEach
    void setUp() {
        service = new ReservaPublicaService(
                reservaRepository, eventoRepository, clientePerfilRepository, sedeRepository,
                tarifaRepository, feriadoRepository, bloqueRepository, configRepository,
                storagePort, ventaRepository, ventaPagoRepository, supabaseAuthFacade,
                configuracionGlobalRepository, auditoria, crearNotificacionPort, sedeScope,
                new ObjectMapper(), resolverAdministradoresPort);
    }

    private ReservaPublica reservaPendiente() {
        return ReservaPublica.builder()
                .id(1L).idCliente(5L).idSede(1L)
                .estado(EstadoReservaPublica.PENDIENTE)
                .canalReserva(CanalReserva.WEB)
                .tipoDia(com.playzone.pems.domain.calendario.model.enums.TipoDia.SEMANA)
                .fechaEvento(LocalDate.of(2026, 9, 1))
                .numeroTicket("TCK-0010")
                .build();
    }

    @Test
    void testCancelarUsaNotificarTransaccionalConTipoReservaCancelada() {
        ReservaPublica reserva = reservaPendiente();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(5L).nombres("Juan").correo("juan@correo.com").build()));
        when(sedeRepository.findById(1L)).thenReturn(Optional.empty());

        service.ejecutar(1L, "Cliente pidió reembolso");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());

        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("RESERVA_CANCELADA", cmd.getTipoCodigo());
        assertEquals(5L, cmd.getDestinatarioClienteId());
        assertEquals(1L, cmd.getEntidadId());
        assertEquals("Cliente pidió reembolso", cmd.getDatosExtra().get("motivo"));
        verify(crearNotificacionPort, never()).notificar(any());
    }

    @Test
    void testCancelarNoPermiteReservaYaCancelada() {
        ReservaPublica reserva = reservaPendiente().toBuilder().estado(EstadoReservaPublica.CANCELADA).build();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThrows(com.playzone.pems.shared.exception.ValidationException.class,
                () -> service.ejecutar(1L, "motivo"));
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    @Test
    void testRechazarPagoIncluyeMotivoEnMetadataJson() {
        ReservaPublica reserva = reservaPendiente();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(ventaPagoRepository.findByVentaId(any())).thenReturn(List.of());
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(5L).nombres("Juan").correo("juan@correo.com").build()));
        when(sedeRepository.findById(1L)).thenReturn(Optional.empty());

        service.rechazarPago(1L, "Comprobante borroso");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());

        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("PAGO_RECHAZADO", cmd.getTipoCodigo());
        assertTrue(cmd.getMetadata().contains("Comprobante borroso"));
    }

    @Test
    void testCrearReservaCopiaLaDuracionHistoricaDeLaTarifaVigente() {
        LocalDate fecha = LocalDate.now().plusDays(5);

        CrearReservaPublicaCommand comando = CrearReservaPublicaCommand.builder()
                .idCliente(5L)
                .idSede(1L)
                .canalReserva(CanalReserva.WEB)
                .fechaEvento(fecha)
                .nombreNino("Ana")
                .edadNino(6)
                .nombreAcompanante("Juan")
                .dniAcompanante("12345678")
                .firmoConsentimiento(true)
                .medioPago("YAPE")
                .build();

        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .idSede(1L)
                .diasMaxReservaPublica(30)
                .aforoMaximo(100)
                .horaCierre(LocalTime.of(20, 0))
                .build());
        when(bloqueRepository.existsBloqueActivoEnFecha(1L, fecha)).thenReturn(false);
        when(eventoRepository.existsActivoBySedeAndFecha(1L, fecha)).thenReturn(false);
        when(reservaRepository.countActivasBySedeAndFecha(1L, fecha)).thenReturn(0);
        when(reservaRepository.countPendientesSinComprobantePorClienteYFecha(5L, fecha)).thenReturn(0);
        when(feriadoRepository.findByFecha(fecha)).thenReturn(Optional.empty());
        when(tarifaRepository.findVigenteBySedeAndTipoDiaAndFecha(eq(1L), any(TipoDia.class), eq(fecha)))
                .thenReturn(Optional.of(Tarifa.builder()
                        .id(9L).idSede(1L).precio(new BigDecimal("25.00")).duracionMinutos(120).activo(true)
                        .build()));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(5L).nombres("Ana Madre").correo("mama@correo.com").build()));
        when(ventaRepository.save(any())).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            return v.toBuilder().id(50L).build();
        });
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sedeRepository.findById(1L)).thenReturn(Optional.empty());

        service.ejecutar(comando);

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(120, captor.getValue().getDuracionHistoricaMinutos());
    }

    @Test
    void testConfirmarPagoUsaNotificarTransaccional() {
        ReservaPublica reserva = reservaPendiente().toBuilder()
                .totalPagado(new java.math.BigDecimal("100.00")).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(5L).nombres("Juan").correo("juan@correo.com").build()));
        when(sedeRepository.findById(1L)).thenReturn(Optional.empty());

        service.confirmarPago(1L, "YAPE");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("PAGO_CONFIRMADO", captor.getValue().getTipoCodigo());
    }

    @Test
    void testReembolsarPorNoShowExigeEstadoVencida() {
        ReservaPublica reserva = reservaPendiente().toBuilder().estado(EstadoReservaPublica.CONFIRMADA).build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));

        assertThrows(com.playzone.pems.shared.exception.ValidationException.class,
                () -> service.reembolsarPorNoShow(1L, "cliente nunca llegó"));
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void testReembolsarPorNoShowCancelaYNotificaInApp() {
        ReservaPublica reserva = reservaPendiente().toBuilder()
                .estado(EstadoReservaPublica.VENCIDA)
                .totalPagado(new BigDecimal("25.00"))
                .ventaId(50L)
                .build();
        when(reservaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(
                ClientePerfil.builder().id(5L).nombres("Juan").correo("juan@correo.com").build()));
        when(sedeRepository.findById(1L)).thenReturn(Optional.empty());

        service.reembolsarPorNoShow(1L, "cliente nunca llegó");

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(EstadoReservaPublica.CANCELADA, captor.getValue().getEstado());
        assertTrue(captor.getValue().getMotivoCancelacion().contains("cliente nunca llegó"));

        ArgumentCaptor<CrearNotificacionCommand> notifCaptor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(notifCaptor.capture());
        assertEquals("RESERVA_REEMBOLSADA", notifCaptor.getValue().getTipoCodigo());
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    @Test
    void testClienteNoPuedeOperarSobreReservaVencida() {
        ReservaPublica reserva = reservaPendiente().toBuilder().estado(EstadoReservaPublica.VENCIDA).build();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.ejecutar(1L, "quiero cancelar"));
        verify(reservaRepository, never()).save(any());
    }
}
