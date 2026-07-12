package com.playzone.pems.application.evento.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
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

import java.time.LocalDate;
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

    private ReservaPublicaService service;

    @BeforeEach
    void setUp() {
        service = new ReservaPublicaService(
                reservaRepository, eventoRepository, clientePerfilRepository, sedeRepository,
                tarifaRepository, feriadoRepository, bloqueRepository, configRepository,
                storagePort, ventaRepository, ventaPagoRepository, supabaseAuthFacade,
                configuracionGlobalRepository, auditoria, crearNotificacionPort, sedeScope,
                new ObjectMapper());
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
}
