package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarPagoCuotaCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarSaldoCommand;
import com.playzone.pems.application.evento.dto.command.SolicitarEventoPrivadoCommand;
import com.playzone.pems.application.evento.port.out.EnviarNotificacionEventoPort;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.BloqueCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.model.TipoEvento;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.TipoEventoRepository;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoPrivadoServiceTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private BloqueCalendarioRepository bloqueRepository;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private ConfiguracionCalendarioRepository configRepository;
    @Mock private EnviarNotificacionEventoPort notificacionPort;
    @Mock private EventoExtraRepository eventoExtraRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private SupabaseAuthFacade supabaseAuthFacade;
    @Mock private ExtraPaqueteRepository extraPaqueteRepository;
    @Mock private ServicioCotizacionRepository servicioCotizacionRepository;
    @Mock private TipoEventoRepository tipoEventoRepository;
    @Mock private EventoCuotaRepository cuotaRepository;
    @Mock private ChecklistEventoRepository checklistRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private EnrutadorCajaService enrutadorCajaService;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private SedeScopeValidator sedeScope;

    private EventoPrivadoService service;

    @BeforeEach
    void setUp() {
        service = new EventoPrivadoService(
                eventoRepository, reservaRepository, clientePerfilRepository, bloqueRepository,
                feriadoRepository, turnoRepository, configRepository, notificacionPort,
                eventoExtraRepository, ventaRepository, ventaPagoRepository, supabaseAuthFacade,
                extraPaqueteRepository, servicioCotizacionRepository, tipoEventoRepository,
                cuotaRepository, checklistRepository, perfilUsuarioRepository, enrutadorCajaService,
                auditoria, crearNotificacionPort, sedeScope, new ObjectMapper());
    }

    private ClientePerfil clientePrueba() {
        return ClientePerfil.builder().id(5L).nombres("Ana").correo("ana@correo.com").build();
    }

    private Turno turnoPrueba() {
        return Turno.builder().id(1L).codigo("TARDE").descripcion("Tarde")
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(18, 0)).build();
    }

    @Test
    void testSolicitarEventoNotificaTransaccionalPresupuestoEnviado() {
        when(supabaseAuthFacade.tieneRol(anyString())).thenReturn(true);
        when(tipoEventoRepository.buscarPorCodigo("CUMPLEANOS")).thenReturn(
                Optional.of(TipoEvento.builder().codigo("CUMPLEANOS").activo(true).build()));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .diasMinEventoPrivado(1).diasMaxEventoPrivado(365).build());
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));
        when(eventoRepository.save(any())).thenAnswer(inv -> {
            EventoPrivado arg = inv.getArgument(0);
            return arg.toBuilder().id(200L).build();
        });
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .build();

        service.ejecutar(comando);

        verify(notificacionPort).notificarAdminNuevaSolicitud(any());

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("EVENTO_PRESUPUESTO_ENVIADO", captor.getValue().getTipoCodigo());
        assertEquals(5L, captor.getValue().getDestinatarioClienteId());
        verify(crearNotificacionPort, never()).notificar(any());
    }

    @Test
    void testConfirmarEventoSinAdelantoNotificaSoloEventoConfirmado() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(300L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .montoAdelanto(BigDecimal.ZERO)
                .build();
        when(eventoRepository.findById(300L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        ConfirmarEventoCommand comando = ConfirmarEventoCommand.builder()
                .idEvento(300L).precioTotal(new BigDecimal("500.00"))
                .montoAdelanto(BigDecimal.ZERO).pagosAdelanto(List.of())
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("EVENTO_CONFIRMADO", captor.getValue().getTipoCodigo());
        assertEquals(5L, captor.getValue().getDestinatarioClienteId());
    }

    @Test
    void testConfirmarEventoConAdelantoNotificaPagoYAlGestor() {
        UUID gestorId = UUID.randomUUID();
        EventoPrivado evento = EventoPrivado.builder()
                .id(301L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .montoAdelanto(BigDecimal.ZERO)
                .build();
        when(eventoRepository.findById(301L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));
        when(ventaRepository.save(any())).thenAnswer(inv -> {
            Venta arg = inv.getArgument(0);
            return arg.toBuilder().id(400L).build();
        });

        ConfirmarEventoCommand comando = ConfirmarEventoCommand.builder()
                .idEvento(301L).precioTotal(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .idUsuarioGestor(gestorId)
                .pagosAdelanto(List.of(com.playzone.pems.application.evento.dto.command.VentaPagoItem.builder()
                        .medioPagoCodigo("YAPE").monto(new BigDecimal("200.00")).build()))
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort, times(3)).notificarTransaccional(captor.capture());
        List<CrearNotificacionCommand> comandos = captor.getAllValues();

        assertTrue(comandos.stream().anyMatch(c -> "EVENTO_CONFIRMADO".equals(c.getTipoCodigo())));
        assertTrue(comandos.stream().anyMatch(c -> "PAGO_ADELANTO_CONFIRMADO".equals(c.getTipoCodigo())));

        CrearNotificacionCommand gestorCmd = comandos.stream()
                .filter(c -> "EVENTO_ADELANTO_RECIBIDO".equals(c.getTipoCodigo()))
                .findFirst().orElseThrow();
        assertEquals(gestorId, gestorCmd.getDestinatarioUsuarioId());
        assertEquals("200.00", gestorCmd.getDatosExtra().get("monto"));
        assertNotNull(gestorCmd.getDatosExtra().get("evento"));
        assertNotNull(gestorCmd.getDatosExtra().get("fecha"));
    }

    @Test
    void testRegistrarPagoCuotaNotificaAbonoAlClienteYSaldoAlGestor() {
        UUID gestorId = UUID.randomUUID();
        EventoCuota cuota = EventoCuota.builder()
                .id(1L).eventoId(500L).numeroCuota(2).monto(new BigDecimal("100.00")).estado("PENDIENTE").build();
        EventoPrivado evento = EventoPrivado.builder()
                .id(500L).idCliente(5L).idSede(1L).idTurno(1L).idUsuarioGestor(gestorId)
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .precioContrato(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .build();
        when(cuotaRepository.findById(1L)).thenReturn(Optional.of(cuota));
        when(eventoRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(evento));
        when(ventaRepository.save(any())).thenAnswer(inv -> {
            Venta arg = inv.getArgument(0);
            return arg.toBuilder().id(401L).build();
        });
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        RegistrarPagoCuotaCommand comando = RegistrarPagoCuotaCommand.builder()
                .idCuota(1L)
                .pagos(List.of(com.playzone.pems.application.evento.dto.command.VentaPagoItem.builder()
                        .medioPagoCodigo("EFECTIVO").monto(new BigDecimal("100.00")).build()))
                .idUsuario(gestorId)
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort, times(2)).notificarTransaccional(captor.capture());
        List<CrearNotificacionCommand> comandos = captor.getAllValues();

        CrearNotificacionCommand gestorCmd = comandos.stream()
                .filter(c -> "EVENTO_SALDO_RECIBIDO".equals(c.getTipoCodigo())).findFirst().orElseThrow();
        assertEquals(gestorId, gestorCmd.getDestinatarioUsuarioId());

        CrearNotificacionCommand clienteCmd = comandos.stream()
                .filter(c -> "EVENTO_ABONO_RECIBIDO".equals(c.getTipoCodigo())).findFirst().orElseThrow();
        assertEquals(5L, clienteCmd.getDestinatarioClienteId());
        assertTrue(clienteCmd.getMetadata().contains("montoAbonado"));
    }

    @Test
    void testRegistrarSaldoNotificaAbonoAlClienteYSaldoAlGestor() {
        UUID gestorId = UUID.randomUUID();
        EventoPrivado evento = EventoPrivado.builder()
                .id(600L).idCliente(5L).idSede(1L).idTurno(1L).idUsuarioGestor(gestorId)
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .precioContrato(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .build();
        when(eventoRepository.findByIdForUpdate(600L)).thenReturn(Optional.of(evento));
        when(ventaRepository.save(any())).thenAnswer(inv -> {
            Venta arg = inv.getArgument(0);
            return arg.toBuilder().id(402L).build();
        });
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        RegistrarSaldoCommand comando = RegistrarSaldoCommand.builder()
                .idEvento(600L).monto(new BigDecimal("300.00")).medioPago("TARJETA").idUsuario(gestorId)
                .build();

        service.registrarSaldo(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort, times(2)).notificarTransaccional(captor.capture());
        List<CrearNotificacionCommand> comandos = captor.getAllValues();

        assertTrue(comandos.stream().anyMatch(c -> "EVENTO_SALDO_RECIBIDO".equals(c.getTipoCodigo())
                && gestorId.equals(c.getDestinatarioUsuarioId())));
        assertTrue(comandos.stream().anyMatch(c -> "EVENTO_ABONO_RECIBIDO".equals(c.getTipoCodigo())
                && Long.valueOf(5L).equals(c.getDestinatarioClienteId())));
    }

    @Test
    void testCancelarEventoNotificaTransaccionalEventoCanceladoAdmin() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(700L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .build();
        when(eventoRepository.findById(700L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        service.ejecutar(700L, "Cliente solicito reembolso");

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificarTransaccional(captor.capture());
        assertEquals("EVENTO_CANCELADO_ADMIN", captor.getValue().getTipoCodigo());
        assertEquals("Cliente solicito reembolso", captor.getValue().getDatosExtra().get("motivo"));
    }
}
