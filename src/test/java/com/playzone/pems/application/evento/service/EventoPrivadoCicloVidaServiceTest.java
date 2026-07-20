package com.playzone.pems.application.evento.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.application.evento.dto.command.SolicitarEventoPrivadoCommand;
import com.playzone.pems.application.evento.dto.command.VentaPagoItem;
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
import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.model.TipoEvento;
import com.playzone.pems.domain.comercial.repository.ExtraPaqueteRepository;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.domain.comercial.repository.TipoEventoRepository;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.EventoServicio;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.ChecklistEventoRepository;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.domain.evento.repository.EventoExtraRepository;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.EventoServicioRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoPrivadoCicloVidaServiceTest {

    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private ClientePerfilRepository clientePerfilRepository;
    @Mock private BloqueCalendarioRepository bloqueRepository;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private TurnoRepository turnoRepository;
    @Mock private ConfiguracionCalendarioRepository configRepository;
    @Mock private EnviarNotificacionEventoPort notificacionPort;
    @Mock private EventoExtraRepository eventoExtraRepository;
    @Mock private EventoServicioRepository eventoServicioRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private SupabaseAuthFacade supabaseAuthFacade;
    @Mock private ExtraPaqueteRepository extraPaqueteRepository;
    @Mock private ServicioCotizacionRepository servicioCotizacionRepository;
    @Mock private ServicioVarianteRepository servicioVarianteRepository;
    @Mock private TipoEventoRepository tipoEventoRepository;
    @Mock private EventoCuotaRepository cuotaRepository;
    @Mock private ChecklistEventoRepository checklistRepository;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private EnrutadorCajaService enrutadorCajaService;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private SedeScopeValidator sedeScope;

    private EventoPrivadoCicloVidaService service;

    @BeforeEach
    void setUp() {
        EventoPrivadoQueryMapper mapper = new EventoPrivadoQueryMapper(
                eventoRepository, clientePerfilRepository, turnoRepository,
                eventoExtraRepository, eventoServicioRepository, cuotaRepository,
                extraPaqueteRepository, perfilUsuarioRepository, ventaRepository, ventaPagoRepository);
        EventoAccesoValidator accesoValidator = new EventoAccesoValidator(supabaseAuthFacade, sedeScope);
        VentaEventoWriter ventaWriter = new VentaEventoWriter(ventaRepository, ventaPagoRepository, enrutadorCajaService);
        SolicitudEventoPrivadoValidator solicitudValidator = new SolicitudEventoPrivadoValidator(
                tipoEventoRepository, configRepository, supabaseAuthFacade, feriadoRepository,
                bloqueRepository, reservaRepository, eventoRepository, turnoRepository);
        EventoServicioExtraWriter extraWriter = new EventoServicioExtraWriter(
                servicioCotizacionRepository, servicioVarianteRepository, eventoServicioRepository, eventoExtraRepository);
        EventoCuotaGenerator cuotaGenerator = new EventoCuotaGenerator(cuotaRepository);

        service = new EventoPrivadoCicloVidaService(
                eventoRepository, notificacionPort, checklistRepository, supabaseAuthFacade,
                auditoria, crearNotificacionPort, sedeScope, accesoValidator, mapper, ventaWriter,
                solicitudValidator, extraWriter, cuotaGenerator);
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
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
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
    void testSolicitarEventoConServiciosPersisteEventoServicioConPrecioCongelado() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
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
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(
                ServicioCotizacion.builder().id(10L).nombre("Show de Titeres")
                        .precioReferencial(new BigDecimal("150.00")).activo(true).orden(0).build()));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .idsServiciosCotizacion(List.of(10L))
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<List<EventoServicio>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventoServicioRepository).saveAll(captor.capture());
        List<EventoServicio> guardados = captor.getValue();
        assertEquals(1, guardados.size());
        EventoServicio guardado = guardados.get(0);
        assertEquals(200L, guardado.getIdEventoPrivado());
        assertEquals(10L, guardado.getIdServicioCotizacion());
        assertEquals("Show de Titeres", guardado.getNombreLibre());
        assertEquals(new BigDecimal("150.00"), guardado.getPrecioAcordado());
        assertTrue(guardado.isIncluido());
    }

    @Test
    void testSolicitarEventoSinServiciosNoPersisteEventoServicio() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
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

        verify(eventoServicioRepository, never()).saveAll(any());
        verify(servicioCotizacionRepository, never()).findAllActivos();
    }

    @Test
    void testSolicitarEventoConAforoMayorAlConfiguradoParaLaSedeLanzaValidationException() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
        when(tipoEventoRepository.buscarPorCodigo("CUMPLEANOS")).thenReturn(
                Optional.of(TipoEvento.builder().codigo("CUMPLEANOS").activo(true).build()));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .diasMinEventoPrivado(1).diasMaxEventoPrivado(365)
                .aforoMaximo(60).edadMinCumple(0).edadMaxCumple(18).build());
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .aforoDeclarado(61)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.ejecutar(comando));
        assertTrue(ex.getMessage().toLowerCase().contains("aforo"));
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void testSolicitarEventoConAforoSuperiorA60PeroDentroDelConfiguradoParaLaSedeNoLanzaExcepcion() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
        when(tipoEventoRepository.buscarPorCodigo("CUMPLEANOS")).thenReturn(
                Optional.of(TipoEvento.builder().codigo("CUMPLEANOS").activo(true).build()));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .diasMinEventoPrivado(1).diasMaxEventoPrivado(365)
                .aforoMaximo(100).edadMinCumple(0).edadMaxCumple(18).build());
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
                .aforoDeclarado(80)
                .build();

        service.ejecutar(comando);

        verify(eventoRepository).save(any());
    }

    @Test
    void testSolicitarEventoConEdadCumpleFueraDelRangoConfiguradoParaLaSedeLanzaValidationException() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
        when(tipoEventoRepository.buscarPorCodigo("CUMPLEANOS")).thenReturn(
                Optional.of(TipoEvento.builder().codigo("CUMPLEANOS").activo(true).build()));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .diasMinEventoPrivado(1).diasMaxEventoPrivado(365)
                .aforoMaximo(60).edadMinCumple(1).edadMaxCumple(15).build());
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .edadCumple(16)
                .build();

        ValidationException ex = assertThrows(ValidationException.class, () -> service.ejecutar(comando));
        assertTrue(ex.getMessage().toLowerCase().contains("edad"));
        verify(eventoRepository, never()).save(any());
    }

    @Test
    void testSolicitarEventoConVarianteSeleccionadaPersistePrecioDeLaVariante() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
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
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(
                ServicioCotizacion.builder().id(10L).nombre("Torta")
                        .precioReferencial(new BigDecimal("50.00")).activo(true).orden(0).build()));
        when(servicioVarianteRepository.findByServicio(10L)).thenReturn(List.of(
                ServicioVariante.builder().id(100L).idServicio(10L).nombre("Grande")
                        .precio(new BigDecimal("120.00")).activo(true).orden(0).build()));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .idsServiciosCotizacion(List.of(10L))
                .variantesSeleccionadas(Map.of(10L, 100L))
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<List<EventoServicio>> captor = ArgumentCaptor.forClass(List.class);
        verify(eventoServicioRepository).saveAll(captor.capture());
        EventoServicio guardado = captor.getValue().get(0);
        assertEquals(10L, guardado.getIdServicioCotizacion());
        assertEquals(100L, guardado.getIdServicioVariante());
        assertEquals("Torta - Grande", guardado.getNombreLibre());
        assertEquals(new BigDecimal("120.00"), guardado.getPrecioAcordado());
    }

    @Test
    void testSolicitarEventoConServicioConVariantesSinSeleccionarLanzaValidationException() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(false);
        when(tipoEventoRepository.buscarPorCodigo("CUMPLEANOS")).thenReturn(
                Optional.of(TipoEvento.builder().codigo("CUMPLEANOS").activo(true).build()));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .diasMinEventoPrivado(1).diasMaxEventoPrivado(365).build());
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));
        when(eventoRepository.save(any())).thenAnswer(inv -> {
            EventoPrivado arg = inv.getArgument(0);
            return arg.toBuilder().id(200L).build();
        });
        when(servicioCotizacionRepository.findAllActivos()).thenReturn(List.of(
                ServicioCotizacion.builder().id(10L).nombre("Torta")
                        .precioReferencial(new BigDecimal("50.00")).activo(true).orden(0).build()));
        when(servicioVarianteRepository.findByServicio(10L)).thenReturn(List.of(
                ServicioVariante.builder().id(100L).idServicio(10L).nombre("Grande")
                        .precio(new BigDecimal("120.00")).activo(true).orden(0).build()));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .idsServiciosCotizacion(List.of(10L))
                .build();

        assertThrows(ValidationException.class, () -> service.ejecutar(comando));
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
                .pagosAdelanto(List.of(VentaPagoItem.builder()
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
    void testConfirmarEventoConCuotasYSinAdelantoGeneraCuotasQueSumanElPrecioTotal() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(900L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(2))
                .montoAdelanto(BigDecimal.ZERO)
                .build();
        when(eventoRepository.findById(900L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));

        ConfirmarEventoCommand comando = ConfirmarEventoCommand.builder()
                .idEvento(900L).precioTotal(new BigDecimal("300.00"))
                .montoAdelanto(BigDecimal.ZERO).pagosAdelanto(List.of())
                .modalidadPago("CUOTAS").numeroCuotas(3)
                .fechaLimitePago(LocalDate.now().plusMonths(1))
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<List<EventoCuota>> captor = ArgumentCaptor.forClass(List.class);
        verify(cuotaRepository).saveAll(captor.capture());
        List<EventoCuota> cuotas = captor.getValue();

        assertEquals(3, cuotas.size());
        BigDecimal total = cuotas.stream().map(EventoCuota::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("300.00").compareTo(total));
        assertTrue(cuotas.stream().allMatch(c -> c.getMonto().compareTo(BigDecimal.ZERO) > 0));
        assertTrue(cuotas.stream().allMatch(c -> c.getEstado() == EstadoCuota.PENDIENTE));
        assertTrue(cuotas.stream().allMatch(c -> c.getVentaId() == null));
        assertEquals(List.of(1, 2, 3), cuotas.stream().map(EventoCuota::getNumeroCuota).sorted().toList());
    }

    @Test
    void testConfirmarEventoConCuotasYConAdelantoGeneraCuotasQueSumanElPrecioTotal() {
        EventoPrivado evento = EventoPrivado.builder()
                .id(901L).idCliente(5L).idSede(1L).idTurno(1L)
                .estado(EstadoEventoPrivado.SOLICITADA)
                .fechaEvento(LocalDate.now().plusMonths(2))
                .montoAdelanto(BigDecimal.ZERO)
                .build();
        when(eventoRepository.findById(901L)).thenReturn(Optional.of(evento));
        when(eventoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(clientePerfilRepository.buscarPorId(5L)).thenReturn(Optional.of(clientePrueba()));
        when(turnoRepository.findById(1L)).thenReturn(Optional.of(turnoPrueba()));
        when(ventaRepository.save(any())).thenAnswer(inv -> {
            Venta arg = inv.getArgument(0);
            return arg.toBuilder().id(410L).build();
        });

        ConfirmarEventoCommand comando = ConfirmarEventoCommand.builder()
                .idEvento(901L).precioTotal(new BigDecimal("300.00"))
                .montoAdelanto(new BigDecimal("60.00"))
                .pagosAdelanto(List.of(VentaPagoItem.builder().medioPagoCodigo("YAPE").monto(new BigDecimal("60.00")).build()))
                .modalidadPago("CUOTAS").numeroCuotas(3)
                .fechaLimitePago(LocalDate.now().plusMonths(1))
                .build();

        service.ejecutar(comando);

        ArgumentCaptor<List<EventoCuota>> captor = ArgumentCaptor.forClass(List.class);
        verify(cuotaRepository).saveAll(captor.capture());
        List<EventoCuota> cuotas = captor.getValue();

        assertEquals(3, cuotas.size());
        BigDecimal total = cuotas.stream().map(EventoCuota::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("300.00").compareTo(total));

        EventoCuota primera = cuotas.stream().filter(c -> c.getNumeroCuota() == 1).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("60.00").compareTo(primera.getMonto()));
        assertEquals(EstadoCuota.PAGADO, primera.getEstado());
        assertEquals(410L, primera.getVentaId());
    }

    @Test
    void testSolicitarEventoComoClienteAjenoAlIdClienteLanzaAccessDeniedException() {
        when(supabaseAuthFacade.tieneRol("CLIENTE")).thenReturn(true);
        when(supabaseAuthFacade.clientePerfilId()).thenReturn(Optional.of(9L));

        SolicitarEventoPrivadoCommand comando = SolicitarEventoPrivadoCommand.builder()
                .idCliente(5L).idSede(1L).idTurno(1L)
                .fechaEvento(LocalDate.now().plusMonths(1))
                .tipoEvento("CUMPLEANOS")
                .build();

        assertThrows(AccessDeniedException.class, () -> service.ejecutar(comando));
        verify(eventoRepository, never()).save(any());
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
