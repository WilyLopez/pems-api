package com.playzone.pems.application.finanzas.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.dto.command.AbrirCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.AnularMovimientoCommand;
import com.playzone.pems.application.finanzas.dto.command.CerrarCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarMovimientoManualCommand;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
import com.playzone.pems.domain.finanzas.repository.ArqueoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.MovimientoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SesionCajaServiceTest {

    @Mock private SesionCajaRepository sesionCajaRepository;
    @Mock private MovimientoCajaRepository movimientoCajaRepository;
    @Mock private ArqueoCajaRepository arqueoCajaRepository;
    @Mock private ConfiguracionGlobalRepository configuracionGlobalRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private RegistrarLogUseCase auditoria;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private ResolverAdministradoresPort resolverAdministradoresPort;
    @Mock private PerfilUsuarioRepository perfilUsuarioRepository;
    @Mock private SedeRepository sedeRepository;

    private SesionCajaService service;

    @BeforeEach
    void setUp() {
        service = new SesionCajaService(
                sesionCajaRepository, movimientoCajaRepository, arqueoCajaRepository,
                configuracionGlobalRepository, authFacade, auditoria,
                crearNotificacionPort, resolverAdministradoresPort, perfilUsuarioRepository, sedeRepository);
    }

    @Test
    void testAbrirNotificaAdministradores() {
        UUID usuarioId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        when(sesionCajaRepository.findAbiertaByUsuario(usuarioId)).thenReturn(Optional.empty());
        when(sesionCajaRepository.save(any())).thenAnswer(inv -> {
            SesionCaja s = inv.getArgument(0);
            return s.toBuilder().id(1L).build();
        });
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(adminId));

        AbrirCajaCommand comando = AbrirCajaCommand.builder()
                .idSede(1L).tipo(TipoSesionCaja.CAJERO).saldoInicial(new BigDecimal("100.00"))
                .idUsuarioApertura(usuarioId).build();

        service.abrir(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("CAJA_APERTURA", captor.getValue().getTipoCodigo());
        assertEquals(adminId, captor.getValue().getDestinatarioUsuarioId());
        assertEquals("Sede Norte", captor.getValue().getDatosExtra().get("sede"));
    }

    private SesionCaja sesionAbierta() {
        return SesionCaja.builder()
                .id(5L).idSede(1L).usuarioId(UUID.randomUUID())
                .estado(EstadoCaja.ABIERTA)
                .saldoInicial(BigDecimal.ZERO).totalIngresos(BigDecimal.ZERO).totalEgresos(BigDecimal.ZERO)
                .build();
    }

    @Test
    void testCerrarConDiferenciaSobreUmbralNotificaAdministradores() {
        SesionCaja sesion = sesionAbierta();
        UUID cierreUsuario = sesion.getUsuarioId();
        UUID adminId = UUID.randomUUID();
        when(sesionCajaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sesion));
        when(sesionCajaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(adminId));

        CerrarCajaCommand comando = CerrarCajaCommand.builder()
                .idSesionCaja(5L).saldoFinal(new BigDecimal("200.00"))
                .idUsuarioCierre(cierreUsuario).observaciones("Diferencia por vueltos mal calculados")
                .build();

        service.cerrar(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("CAJA_CIERRE_DISCREPANCIA", captor.getValue().getTipoCodigo());
        assertEquals(adminId, captor.getValue().getDestinatarioUsuarioId());
    }

    @Test
    void testCerrarSinDiferenciaNoNotifica() {
        SesionCaja sesion = sesionAbierta();
        when(sesionCajaRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sesion));
        when(sesionCajaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CerrarCajaCommand comando = CerrarCajaCommand.builder()
                .idSesionCaja(5L).saldoFinal(BigDecimal.ZERO)
                .idUsuarioCierre(sesion.getUsuarioId())
                .build();

        service.cerrar(comando);

        verify(crearNotificacionPort, never()).notificar(any());
    }

    @Test
    void testRegistrarMovimientoGrandeNotificaAdministradores() {
        SesionCaja sesion = sesionAbierta();
        UUID adminId = UUID.randomUUID();
        when(sesionCajaRepository.findById(5L)).thenReturn(Optional.of(sesion));
        when(authFacade.tieneRol(anyString())).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(sesion.getUsuarioId()));
        when(movimientoCajaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sesionCajaRepository.incrementarIngresosSiAbierta(eq(5L), any())).thenReturn(1);
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(adminId));

        RegistrarMovimientoManualCommand comando = RegistrarMovimientoManualCommand.builder()
                .idSesionCaja(5L).tipo(TipoMovimientoCaja.INGRESO)
                .concepto("Venta grande").monto(new BigDecimal("600.00"))
                .idUsuarioRegistra(sesion.getUsuarioId())
                .build();

        service.registrarMovimiento(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("CAJA_MOVIMIENTO_GRANDE", captor.getValue().getTipoCodigo());
    }

    @Test
    void testRegistrarMovimientoPequenoNoNotifica() {
        SesionCaja sesion = sesionAbierta();
        when(sesionCajaRepository.findById(5L)).thenReturn(Optional.of(sesion));
        when(authFacade.tieneRol(anyString())).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(sesion.getUsuarioId()));
        when(movimientoCajaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sesionCajaRepository.incrementarIngresosSiAbierta(eq(5L), any())).thenReturn(1);

        RegistrarMovimientoManualCommand comando = RegistrarMovimientoManualCommand.builder()
                .idSesionCaja(5L).tipo(TipoMovimientoCaja.INGRESO)
                .concepto("Venta pequeña").monto(new BigDecimal("10.00"))
                .idUsuarioRegistra(sesion.getUsuarioId())
                .build();

        service.registrarMovimiento(comando);

        verify(crearNotificacionPort, never()).notificar(any());
    }

    @Test
    void testAnularMovimientoGrandeNotificaAdministradores() {
        SesionCaja sesion = sesionAbierta();
        UUID adminId = UUID.randomUUID();
        MovimientoCaja original = MovimientoCaja.builder()
                .id(20L).idSesionCaja(5L).tipo(TipoMovimientoCaja.INGRESO)
                .monto(new BigDecimal("700.00")).esManual(true).build();
        when(movimientoCajaRepository.findById(20L)).thenReturn(Optional.of(original));
        when(sesionCajaRepository.findById(5L)).thenReturn(Optional.of(sesion));
        when(authFacade.tieneRol(anyString())).thenReturn(false);
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(sesion.getUsuarioId()));
        when(movimientoCajaRepository.existsContraasientoPara(20L)).thenReturn(false);
        when(movimientoCajaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(sesionCajaRepository.incrementarIngresosSiAbierta(eq(5L), any())).thenReturn(1);
        when(sedeRepository.findById(1L)).thenReturn(Optional.of(Sede.builder().id(1L).nombre("Sede Norte").build()));
        when(resolverAdministradoresPort.obtenerIdsAdministradoresActivos()).thenReturn(List.of(adminId));

        AnularMovimientoCommand comando = AnularMovimientoCommand.builder()
                .idMovimiento(20L).motivo("Registrado por error").idUsuarioAnula(sesion.getUsuarioId())
                .build();

        service.anularMovimiento(comando);

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        assertEquals("CAJA_MOVIMIENTO_GRANDE", captor.getValue().getTipoCodigo());
    }
}
