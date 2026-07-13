package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
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
import java.time.LocalTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaAutoCancelacionJob {

    private static final String MOTIVO =
            "Cancelada automáticamente: no se confirmó el pago antes del cierre del local en la fecha reservada.";

    private final SedeRepository sedeRepository;
    private final ReservaPublicaRepository reservaRepository;
    private final ConfiguracionCalendarioRepository configRepository;
    private final CrearNotificacionPort crearNotificacionPort;
    private final RegistrarLogUseCase auditoria;

    @Scheduled(cron = "0 */30 * * * *", zone = "America/Lima")
    @Transactional
    public void cancelarPendientesVencidas() {
        try {
            LocalDate hoy = FechaUtil.hoy();
            LocalTime ahora = FechaUtil.ahora().toLocalTime();

            for (Sede sede : sedeRepository.findAllActivas()) {
                ConfiguracionCalendario config = configRepository.obtener(sede.getId());
                if (config.getHoraCierre() == null || ahora.isBefore(config.getHoraCierre())) {
                    continue;
                }
                for (ReservaPublica reserva : reservaRepository.findPendientesBySedeAndFecha(sede.getId(), hoy)) {
                    cancelarPorNoPago(reserva);
                }
            }
        } catch (Exception e) {
            log.error("[ReservaAutoCancelacionJob] Error en cancelarPendientesVencidas: {}", e.getMessage(), e);
        }
    }

    private void cancelarPorNoPago(ReservaPublica reserva) {
        ReservaPublica cancelada = reserva.toBuilder()
                .estado(EstadoReservaPublica.CANCELADA)
                .motivoCancelacion(MOTIVO)
                .build();
        reservaRepository.save(cancelada);

        crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                .tipoCodigo("RESERVA_CANCELADA")
                .destinatarioClienteId(reserva.getIdCliente())
                .entidadTipo("reserva_publica")
                .entidadId(reserva.getId())
                .datosExtra(Map.of(
                        "fecha", reserva.getFechaEvento().toString(),
                        "motivo", MOTIVO))
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                null, AuditoriaConstants.ACCION_CANCELAR, AuditoriaConstants.MOD_RESERVAS,
                "ReservaPublica", reserva.getId(),
                reserva.getEstado().getCodigo(), "CANCELADA",
                "Reserva #" + reserva.getId() + " cancelada automáticamente por falta de pago",
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        log.info("[ReservaAutoCancelacionJob] Reserva #{} cancelada automáticamente por falta de pago", reserva.getId());
    }
}
