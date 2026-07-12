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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecordatorioReservaJob {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final SedeRepository sedeRepository;
    private final ReservaPublicaRepository reservaRepository;
    private final EventoPrivadoRepository eventoRepository;
    private final CrearNotificacionPort crearNotificacionPort;

    @Scheduled(cron = "0 0 10 * * *", zone = "America/Lima")
    public void enviarRecordatorios() {
        try {
            LocalDate manana = LocalDate.now(LIMA).plusDays(1);
            LocalDate en3Dias = LocalDate.now(LIMA).plusDays(3);

            for (Sede sede : sedeRepository.findAllActivas()) {
                recordarReservas(sede, manana);
                recordarEventos(sede, en3Dias);
            }
        } catch (Exception e) {
            log.error("[RecordatorioReservaJob] Error en enviarRecordatorios: {}", e.getMessage(), e);
        }
    }

    private void recordarReservas(Sede sede, LocalDate fecha) {
        for (ReservaPublica reserva : reservaRepository.findConfirmadasBySedeAndFecha(sede.getId(), fecha)) {
            crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                    .tipoCodigo("RESERVA_RECORDATORIO")
                    .destinatarioClienteId(reserva.getIdCliente())
                    .entidadTipo("RESERVA")
                    .entidadId(reserva.getId())
                    .datosExtra(Map.of(
                            "fecha", reserva.getFechaEvento().toString(),
                            "sede", sede.getNombre(),
                            "ticket", reserva.getNumeroTicket() != null ? reserva.getNumeroTicket() : ""))
                    .build());
        }
    }

    private void recordarEventos(Sede sede, LocalDate fecha) {
        for (EventoPrivado evento : eventoRepository.findBySedeAndFecha(sede.getId(), fecha)) {
            if (evento.getEstado() != EstadoEventoPrivado.CONFIRMADA) continue;
            crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                    .tipoCodigo("EVENTO_RECORDATORIO_3DIAS")
                    .destinatarioClienteId(evento.getIdCliente())
                    .entidadTipo("evento_privado")
                    .entidadId(evento.getId())
                    .datosExtra(Map.of(
                            "fecha", evento.getFechaEvento().toString(),
                            "sede", sede.getNombre()))
                    .build());
        }
    }
}
