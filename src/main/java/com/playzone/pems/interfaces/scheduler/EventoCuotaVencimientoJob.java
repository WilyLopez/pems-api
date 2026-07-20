package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.domain.evento.model.EventoCuota;
import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import com.playzone.pems.domain.evento.repository.EventoCuotaRepository;
import com.playzone.pems.shared.util.FechaUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoCuotaVencimientoJob {

    private final EventoCuotaRepository cuotaRepository;
    private final RegistrarLogUseCase auditoria;

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Lima")
    @Transactional
    public void vencerCuotasPendientes() {
        try {
            LocalDate hoy = FechaUtil.hoy();
            for (EventoCuota cuota : cuotaRepository.findPendientesVencidosAntes(hoy)) {
                vencerCuota(cuota);
            }
        } catch (Exception e) {
            log.error("[EventoCuotaVencimientoJob] Error en vencerCuotasPendientes: {}", e.getMessage(), e);
        }
    }

    private void vencerCuota(EventoCuota cuota) {
        String estadoAnterior = cuota.getEstado().getCodigo();
        cuotaRepository.save(cuota.toBuilder().estado(EstadoCuota.VENCIDO).build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                null, AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_EVENTOS,
                "EventoCuota", cuota.getId(),
                estadoAnterior, EstadoCuota.VENCIDO.getCodigo(),
                "Cuota #" + cuota.getNumeroCuota() + " del evento #" + cuota.getEventoId() + " marcada como vencida",
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        log.info("[EventoCuotaVencimientoJob] Cuota #{} (evento #{}) vencida", cuota.getId(), cuota.getEventoId());
    }
}
