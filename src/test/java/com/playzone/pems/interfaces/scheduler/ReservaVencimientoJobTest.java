package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.shared.util.FechaUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaVencimientoJobTest {

    @Mock private SedeRepository sedeRepository;
    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private CrearNotificacionPort crearNotificacionPort;
    @Mock private RegistrarLogUseCase auditoria;

    private ReservaVencimientoJob job;

    private ReservaPublica reservaSinIngreso(LocalDate fecha) {
        return ReservaPublica.builder()
                .id(1L).idCliente(5L).idSede(1L)
                .estado(EstadoReservaPublica.CONFIRMADA)
                .canalReserva(CanalReserva.WEB)
                .fechaEvento(fecha)
                .ingresado(false)
                .numeroTicket("TCK-0030")
                .build();
    }

    @Test
    void testVenceReservasConfirmadasSinIngresoDeFechasPasadas() {
        job = new ReservaVencimientoJob(sedeRepository, reservaRepository, crearNotificacionPort, auditoria);

        Sede sede = Sede.builder().id(1L).build();
        when(sedeRepository.findAllActivas()).thenReturn(List.of(sede));

        LocalDate ayer = FechaUtil.hoy().minusDays(1);
        ReservaPublica reserva = reservaSinIngreso(ayer);
        when(reservaRepository.findConfirmadasSinIngresoAntesDe(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of(reserva));
        when(reservaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        job.vencerReservasSinIngreso();

        ArgumentCaptor<ReservaPublica> captor = ArgumentCaptor.forClass(ReservaPublica.class);
        verify(reservaRepository).save(captor.capture());
        assertEquals(EstadoReservaPublica.VENCIDA, captor.getValue().getEstado());

        ArgumentCaptor<CrearNotificacionCommand> notifCaptor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(notifCaptor.capture());
        assertEquals("RESERVA_VENCIDA", notifCaptor.getValue().getTipoCodigo());
    }

    @Test
    void testNoVenceNadaSiNoHayReservasElegibles() {
        job = new ReservaVencimientoJob(sedeRepository, reservaRepository, crearNotificacionPort, auditoria);

        Sede sede = Sede.builder().id(1L).build();
        when(sedeRepository.findAllActivas()).thenReturn(List.of(sede));
        when(reservaRepository.findConfirmadasSinIngresoAntesDe(eq(1L), any(LocalDate.class)))
                .thenReturn(List.of());

        job.vencerReservasSinIngreso();

        verify(reservaRepository, never()).save(any());
        verify(crearNotificacionPort, never()).notificar(any());
    }
}
