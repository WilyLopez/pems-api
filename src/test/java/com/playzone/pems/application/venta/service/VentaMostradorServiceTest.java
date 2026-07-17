package com.playzone.pems.application.venta.service;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.finanzas.service.EnrutadorCajaService;
import com.playzone.pems.application.venta.dto.command.NinoMostradorCommand;
import com.playzone.pems.application.venta.dto.command.PagoMostradorCommand;
import com.playzone.pems.application.venta.dto.command.RegistrarVentaMostradorCommand;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.calendario.repository.FeriadoRepository;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.domain.promocion.repository.PromocionRepository;
import com.playzone.pems.domain.venta.model.Venta;
import com.playzone.pems.domain.venta.repository.VentaPagoRepository;
import com.playzone.pems.domain.venta.repository.VentaRepository;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VentaMostradorServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private VentaPagoRepository ventaPagoRepository;
    @Mock private SesionCajaRepository sesionCajaRepository;
    @Mock private EnrutadorCajaService enrutadorCajaService;
    @Mock private TarifaRepository tarifaRepository;
    @Mock private FeriadoRepository feriadoRepository;
    @Mock private PromocionRepository promocionRepository;
    @Mock private SupabaseAuthFacade authFacade;
    @Mock private ConfiguracionCalendarioRepository configRepository;
    @Mock private RegistrarLogUseCase auditoria;

    private VentaMostradorService service;

    @BeforeEach
    void setUp() {
        service = new VentaMostradorService(
                ventaRepository, reservaRepository, ventaPagoRepository, sesionCajaRepository,
                enrutadorCajaService, tarifaRepository, feriadoRepository, promocionRepository,
                authFacade, configRepository, auditoria);
    }

    private RegistrarVentaMostradorCommand.RegistrarVentaMostradorCommandBuilder comandoBase(LocalDate fechaVisita) {
        return RegistrarVentaMostradorCommand.builder()
                .sedeId(1L)
                .fechaVisita(fechaVisita)
                .ninos(List.of(NinoMostradorCommand.builder().nombreNino("Ana").edadNino(6).build()))
                .pagos(List.of(PagoMostradorCommand.builder()
                        .medioPago("YAPE").monto(new BigDecimal("25.00")).build()))
                .efectivoRecibido(BigDecimal.ZERO)
                .actaFirmada(true);
    }

    private void mockDependenciasComunes(LocalDate fecha, LocalTime apertura, LocalTime cierre) {
        UUID usuarioActual = UUID.randomUUID();
        when(authFacade.usuarioActualId()).thenReturn(Optional.of(usuarioActual));
        when(sesionCajaRepository.findAbiertaByUsuarioAndSede(eq(usuarioActual), eq(1L)))
                .thenReturn(Optional.of(mock(SesionCaja.class)));
        when(configRepository.obtener(1L)).thenReturn(ConfiguracionCalendario.builder()
                .idSede(1L)
                .horaApertura(apertura)
                .horaCierre(cierre)
                .build());
        when(feriadoRepository.existsByFecha(fecha)).thenReturn(false);
        when(tarifaRepository.findVigenteBySedeAndTipoDiaAndFecha(eq(1L), any(TipoDia.class), eq(fecha)))
                .thenReturn(Optional.of(Tarifa.builder()
                        .id(9L).idSede(1L).precio(new BigDecimal("25.00")).duracionMinutos(120).activo(true)
                        .build()));
        when(ventaRepository.save(any())).thenAnswer(inv -> {
            Venta v = inv.getArgument(0);
            return v.toBuilder().id(70L).build();
        });
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ventaPagoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void testVentaParaFechaFuturaCopiaDuracionHistoricaSinMarcarIngreso() {
        LocalDate fechaFutura = LocalDate.now().plusDays(3);
        mockDependenciasComunes(fechaFutura, LocalTime.of(10, 0), LocalTime.of(20, 0));

        service.registrar(comandoBase(fechaFutura).build());

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        verify(reservaRepository).save(captor.capture());
        ReservaPublica guardada = captor.getValue();

        assertEquals(120, guardada.getDuracionHistoricaMinutos());
        assertFalse(guardada.isIngresado());
        assertNull(guardada.getPermanenciaFinAt());
    }

    @Test
    void testVentaMismoDiaConLocalAbiertoCalculaPermanenciaFinDesdeElIngreso() {
        LocalDate hoy = LocalDate.now();
        mockDependenciasComunes(hoy, LocalTime.of(0, 1), LocalTime.of(23, 59));

        service.registrar(comandoBase(hoy).build());

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        verify(reservaRepository).save(captor.capture());
        ReservaPublica guardada = captor.getValue();

        assertTrue(guardada.isIngresado());
        assertEquals(120, guardada.getDuracionHistoricaMinutos());
        assertEquals(guardada.getIngresoAt().plusMinutes(120), guardada.getPermanenciaFinAt());
    }
}
