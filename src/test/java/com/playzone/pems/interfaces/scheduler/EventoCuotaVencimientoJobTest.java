package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.shared.util.FechaUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventoCuotaVencimientoJobTest {

    @Mock private EventoCuotaRepository cuotaRepository;
    @Mock private RegistrarLogUseCase auditoria;

    private EventoCuota cuotaPendiente(LocalDate fechaVencimiento) {
        return EventoCuota.builder()
                .id(1L).eventoId(500L).numeroCuota(2)
                .monto(new BigDecimal("100.00"))
                .fechaVencimiento(fechaVencimiento)
                .estado(EstadoCuota.PENDIENTE)
                .build();
    }

    @Test
    void testVenceCuotasPendientesConFechaDeVencimientoPasada() {
        EventoCuotaVencimientoJob job = new EventoCuotaVencimientoJob(cuotaRepository, auditoria);

        LocalDate ayer = FechaUtil.hoy().minusDays(1);
        EventoCuota cuota = cuotaPendiente(ayer);
        when(cuotaRepository.findPendientesVencidosAntes(any(LocalDate.class)))
                .thenReturn(List.of(cuota));
        when(cuotaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.vencerCuotasPendientes();

        ArgumentCaptor<EventoCuota> captor = ArgumentCaptor.forClass(EventoCuota.class);
        verify(cuotaRepository).save(captor.capture());
        assertEquals(EstadoCuota.VENCIDO, captor.getValue().getEstado());
    }

    @Test
    void testNoVenceNadaSiNoHayCuotasElegibles() {
        EventoCuotaVencimientoJob job = new EventoCuotaVencimientoJob(cuotaRepository, auditoria);

        when(cuotaRepository.findPendientesVencidosAntes(any(LocalDate.class)))
                .thenReturn(List.of());

        job.vencerCuotasPendientes();

        verify(cuotaRepository, never()).save(any());
    }
}
