package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.evento.dto.command.RegistrarPagoCuotaCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarSaldoCommand;
import com.playzone.pems.application.evento.dto.command.VentaPagoItem;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoPagoServiceTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private EventoCuotaRepository cuotaRepository;
    @Mock private SedeScopeValidator sedeScope;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private EnrutadorCajaService enrutadorCajaService;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private EventoExtraRepository eventoExtraRepository;
    @Mock private EventoServicioRepository eventoServicioRepository;
    @Mock private ExtraPaqueteRepository extraPaqueteRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private VentaRepository ventaRepository;

    private EventoPagoService service;

    @BeforeEach
    void setUp() {
        EventoPrivadoQueryMapper mapper = new EventoPrivadoQueryMapper(
                eventoRepository, clientePerfilRepository, turnoRepository,
                eventoExtraRepository, eventoServicioRepository, cuotaRepository,
                extraPaqueteRepository, perfilUsuarioRepository, ventaRepository, ventaPagoRepository);
        VentaEventoWriter ventaWriter = new VentaEventoWriter(ventaRepository, ventaPagoRepository, enrutadorCajaService);

        service = new EventoPagoService(eventoRepository, cuotaRepository, sedeScope, ventaPagoRepository,
                enrutadorCajaService, crearNotificacionPort, new ObjectMapper(), mapper, ventaWriter);
    }

    private ClientePerfil clientePrueba() {
        return ClientePerfil.builder().id(5L).nombres("Ana").correo("ana@correo.com").build();
    }

    private Turno turnoPrueba() {
        return Turno.builder().id(1L).codigo("TARDE").descripcion("Tarde")
                .horaInicio(LocalTime.of(15, 0)).horaFin(LocalTime.of(18, 0)).build();
    }

    @Test
    void testRegistrarPagoCuotaNotificaAbonoAlClienteYSaldoAlGestor() {
        UUID gestorId = UUID.randomUUID();
        EventoCuota cuota = EventoCuota.builder()
                .id(1L).eventoId(500L).numeroCuota(2).monto(new BigDecimal("100.00")).estado(EstadoCuota.PENDIENTE).build();
        EventoPrivado evento = EventoPrivado.builder()
                .id(500L).idCliente(5L).idSede(1L).idTurno(1L).idUsuarioGestor(gestorId)
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .precioContrato(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .build();
        when(cuotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cuota));
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
                .pagos(List.of(VentaPagoItem.builder()
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
    void testRegistrarPagoCuotaBloqueaLaCuotaConLockPesimistaYNoUsaFindByIdSinLock() {
        UUID gestorId = UUID.randomUUID();
        EventoCuota cuota = EventoCuota.builder()
                .id(1L).eventoId(500L).numeroCuota(2).monto(new BigDecimal("100.00")).estado(EstadoCuota.PENDIENTE).build();
        EventoPrivado evento = EventoPrivado.builder()
                .id(500L).idCliente(5L).idSede(1L).idTurno(1L).idUsuarioGestor(gestorId)
                .estado(EstadoEventoPrivado.CONFIRMADA)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .precioContrato(new BigDecimal("500.00"))
                .montoAdelanto(new BigDecimal("200.00"))
                .build();
        when(cuotaRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(cuota));
        when(eventoRepository.findByIdForUpdate(500L)).thenReturn(Optional.of(evento));
        when(ventaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, Venta.class).toBuilder().id(401L).build());
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        RegistrarPagoCuotaCommand comando = RegistrarPagoCuotaCommand.builder()
                .idCuota(1L)
                .pagos(List.of(VentaPagoItem.builder().medioPagoCodigo("EFECTIVO").monto(new BigDecimal("100.00")).build()))
                .idUsuario(gestorId)
                .build();

        service.ejecutar(comando);

        verify(cuotaRepository).findByIdForUpdate(1L);
        verify(cuotaRepository, never()).findById(anyLong());
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
}
