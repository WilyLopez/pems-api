package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.evento.model.ReservaPublica;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.domain.evento.repository.ReservaPublicaRepository;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.shared.util.FechaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaVencimientoJob {

    private final SedeRepository sedeRepository;
    private final ReservaPublicaRepository reservaRepository;
    private final CrearNotificacionPort crearNotificacionPort;
    private final RegistrarLogUseCase auditoria;

    @Scheduled(cron = "0 15 3 * * *", zone = "America/Lima")
    @Transactional
    public void vencerReservasSinIngreso() {
        try {
            LocalDate hoy = FechaUtil.hoy();
            for (Sede sede : sedeRepository.findAllActivas()) {
                for (ReservaPublica reserva : reservaRepository.findConfirmadasSinIngresoAntesDe(sede.getId(), hoy)) {
                    vencerPorNoShow(reserva);
                }
            }
        } catch (Exception e) {
            log.error("[ReservaVencimientoJob] Error en vencerReservasSinIngreso: {}", e.getMessage(), e);
        }
    }

    private void vencerPorNoShow(ReservaPublica reserva) {
        ReservaPublica vencida = reserva.toBuilder()
                .estado(EstadoReservaPublica.VENCIDA)
                .build();
        reservaRepository.save(vencida);

        crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                .tipoCodigo("RESERVA_VENCIDA")
                .destinatarioClienteId(reserva.getIdCliente())
                .entidadTipo("reserva_publica")
                .entidadId(reserva.getId())
                .datosExtra(Map.of("fecha", reserva.getFechaEvento().toString()))
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                null, AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_RESERVAS,
                "ReservaPublica", reserva.getId(),
                reserva.getEstado().getCodigo(), "VENCIDA",
                "Reserva #" + reserva.getId() + " marcada como vencida por no presentarse",
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        log.info("[ReservaVencimientoJob] Reserva #{} vencida por no presentarse", reserva.getId());
    }
}
