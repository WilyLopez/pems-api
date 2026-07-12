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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
        ReservaPublica reserva = reservaConfirmada();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutar(1L, UUID.randomUUID());

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("RESERVA_INGRESO_CONFIRMADO", captor.getValue().getTipoCodigo());
        assertEquals(5L, captor.getValue().getDestinatarioClienteId());
        verify(crearNotificacionPort, never()).notificarTransaccional(any());
    }

    @Test
    void testMarcarEntradaRegistraAuditoriaYNotifica() {
        ReservaPublica reserva = reservaConfirmada();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.marcarEntrada(1L);

        verify(auditoria).ejecutar(any());

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("RESERVA_INGRESO_CONFIRMADO", captor.getValue().getTipoCodigo());
    }

    @Test
    void testEditarFechaNotificaTransaccionalReservaReprogramada() {
        ReservaPublica reserva = reservaConfirmada();
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));
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
