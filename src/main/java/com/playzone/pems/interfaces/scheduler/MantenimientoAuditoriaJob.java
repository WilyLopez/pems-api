package com.playzone.pems.interfaces.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class MantenimientoAuditoriaJob {

    private final JdbcTemplate jdbc;

    @Scheduled(cron = "0 0 2 * * *", zone = "America/Lima")
    public void ejecutarMantenimientoDiario() {
        try {
            List<Map<String, Object>> resultado = jdbc.queryForList("SELECT * FROM app.mantenimiento_diario()");
            for (Map<String, Object> fila : resultado) {
                log.info("[MantenimientoAuditoriaJob] {}: {} registro(s)", fila.get("tarea"), fila.get("registros_afectados"));
            }
        } catch (Exception e) {
            log.error("[MantenimientoAuditoriaJob] Error en ejecutarMantenimientoDiario: {}", e.getMessage(), e);
        }
    }
}
