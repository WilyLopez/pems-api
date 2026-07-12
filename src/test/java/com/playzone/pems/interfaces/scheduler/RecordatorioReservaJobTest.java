package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.evento.model.EventoPrivado;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.repository.EventoPrivadoRepository;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecordatorioReservaJobTest {

    @Mock private SedeRepository sedeRepository;
    @Mock private ReservaPublicaRepository reservaRepository;
    @Mock private EventoPrivadoRepository eventoRepository;
    @Mock private CrearNotificacionPort crearNotificacionPort;

    private RecordatorioReservaJob job;

    @BeforeEach
    void setUp() {
        job = new RecordatorioReservaJob(sedeRepository, reservaRepository, eventoRepository, crearNotificacionPort);
    }

    @Test
    void testEnviaRecordatorioDeReservaConfirmadaParaManana() {
        Sede sede = Sede.builder().id(1L).nombre("Sede Norte").build();
        when(sedeRepository.findAllActivas()).thenReturn(List.of(sede));

        LocalDate manana = LocalDate.now().plusDays(1);
        ReservaPublica reserva = ReservaPublica.builder()
                .id(10L).idCliente(3L).idSede(1L).fechaEvento(manana).numeroTicket("TCK-0010").build();
        when(reservaRepository.findConfirmadasBySedeAndFecha(1L, manana)).thenReturn(List.of(reserva));
        when(eventoRepository.findBySedeAndFecha(eq(1L), any())).thenReturn(List.of());

        job.enviarRecordatorios();

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("RESERVA_RECORDATORIO", cmd.getTipoCodigo());
        assertEquals(3L, cmd.getDestinatarioClienteId());
    }

    @Test
    void testEnviaRecordatorioDeEventoConfirmadoEnTresDiasYOmiteNoConfirmados() {
        Sede sede = Sede.builder().id(1L).nombre("Sede Norte").build();
        when(sedeRepository.findAllActivas()).thenReturn(List.of(sede));
        when(reservaRepository.findConfirmadasBySedeAndFecha(eq(1L), any())).thenReturn(List.of());

        LocalDate en3Dias = LocalDate.now().plusDays(3);
        EventoPrivado confirmado = EventoPrivado.builder()
                .id(20L).idCliente(4L).idSede(1L).estado(EstadoEventoPrivado.CONFIRMADA).fechaEvento(en3Dias).build();
        EventoPrivado solicitado = EventoPrivado.builder()
                .id(21L).idCliente(5L).idSede(1L).estado(EstadoEventoPrivado.SOLICITADA).fechaEvento(en3Dias).build();
        when(eventoRepository.findBySedeAndFecha(1L, en3Dias)).thenReturn(List.of(confirmado, solicitado));

        job.enviarRecordatorios();

        ArgumentCaptor<CrearNotificacionCommand> captor = ArgumentCaptor.forClass(CrearNotificacionCommand.class);
        verify(crearNotificacionPort).notificar(captor.capture());
        CrearNotificacionCommand cmd = captor.getValue();
        assertEquals("EVENTO_RECORDATORIO_3DIAS", cmd.getTipoCodigo());
        assertEquals(4L, cmd.getDestinatarioClienteId());
    }

    @Test
    void testErrorEnUnaSedeNoInterrumpeElJob() {
        when(sedeRepository.findAllActivas()).thenThrow(new RuntimeException("fallo de BD"));

        assertDoesNotThrow(() -> job.enviarRecordatorios());
        verify(crearNotificacionPort, never()).notificar(any());
    }
}
