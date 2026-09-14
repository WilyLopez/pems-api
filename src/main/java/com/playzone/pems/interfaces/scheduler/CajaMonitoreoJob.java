package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.calendario.model.ConfiguracionCalendario;
import com.playzone.pems.domain.calendario.repository.ConfiguracionCalendarioRepository;
import com.playzone.pems.domain.configuracion.model.ConfiguracionGlobal;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.domain.finanzas.model.MovimientoCaja;
import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import com.playzone.pems.domain.finanzas.repository.MovimientoCajaRepository;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CajaMonitoreoJob {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final String CLAVE_UMBRAL_HORAS = "CAJA_UMBRAL_HORAS_ABIERTA";
    private static final int UMBRAL_HORAS_DEFECTO = 12;

    private final SesionCajaRepository          sesionCajaRepository;
    private final MovimientoCajaRepository      movimientoCajaRepository;
    private final ConfiguracionGlobalRepository configuracionGlobalRepository;
    private final ConfiguracionCalendarioRepository configuracionCalendarioRepository;
    private final CrearNotificacionPort         crearNotificacionPort;
    private final ResolverAdministradoresPort   resolverAdministradoresPort;
    private final PerfilUsuarioRepository       perfilUsuarioRepository;
    private final SedeRepository                sedeRepository;

    @Scheduled(cron = "0 0 * * * *", zone = "America/Lima")
    @Transactional(readOnly = true)
    public void monitorearSesionesAbiertas() {
        try {
            List<SesionCaja> abiertas = sesionCajaRepository.findAllAbiertas();
            int umbralHoras = umbralHoras();
            for (SesionCaja sesion : abiertas) {
                boolean alertaPorHorario = verificarHorarioCierre(sesion);
                if (!alertaPorHorario) {
                    verificarDuracionExcesiva(sesion, umbralHoras);
                }
                verificarReconciliacion(sesion);
            }
        } catch (Exception e) {
            log.error("[CajaMonitoreoJob] Error en monitorearSesionesAbiertas: {}", e.getMessage(), e);
        }
    }

    private void verificarDuracionExcesiva(SesionCaja sesion, int umbralHoras) {
        if (sesion.getFechaApertura() == null) return;
        long horasAbierta = Duration.between(sesion.getFechaApertura(), OffsetDateTime.now(LIMA)).toHours();
        if (horasAbierta < umbralHoras) return;

        notificarAdmins("CAJA_SESION_PROLONGADA", Map.of(
                "usuario", nombreUsuario(sesion.getUsuarioId()),
                "tipo", sesion.getTipo().toString(),
                "sede", nombreSede(sesion.getIdSede()),
                "horas", String.valueOf(horasAbierta)));

        log.warn("[CajaMonitoreoJob] Sesion de caja #{} abierta hace {}h (umbral {}h)",
                sesion.getId(), horasAbierta, umbralHoras);
    }

    /**
     * Alerta si la sesion sigue abierta mas de 1 hora despues de la hora de
     * cierre configurada para su sede. Devuelve true si notifico, para que
     * el llamador evite disparar tambien la alerta generica de umbral de
     * horas para la misma sesion en la misma corrida.
     */
    private boolean verificarHorarioCierre(SesionCaja sesion) {
        if (sesion.getFechaApertura() == null) return false;
        ConfiguracionCalendario config;
        try {
            config = configuracionCalendarioRepository.obtener(sesion.getIdSede());
        } catch (Exception e) {
            return false;
        }
        if (config == null || config.getHoraCierre() == null) return false;

        OffsetDateTime ahora = OffsetDateTime.now(LIMA);
        OffsetDateTime cierreHoy = ahora.toLocalDate().atTime(config.getHoraCierre())
                .atZone(LIMA).toOffsetDateTime();
        OffsetDateTime limiteAlerta = cierreHoy.plusHours(1);
        if (ahora.isBefore(limiteAlerta) || sesion.getFechaApertura().isAfter(limiteAlerta)) {
            return false;
        }

        long horasDesdeCierre = Duration.between(cierreHoy, ahora).toHours();
        notificarAdmins("CAJA_SESION_PROLONGADA", Map.of(
                "usuario", nombreUsuario(sesion.getUsuarioId()),
                "tipo", sesion.getTipo().toString(),
                "sede", nombreSede(sesion.getIdSede()),
                "horas", String.valueOf(horasDesdeCierre)));

        log.warn("[CajaMonitoreoJob] Sesion de caja #{} sigue abierta {}h despues del horario de cierre",
                sesion.getId(), horasDesdeCierre);
        return true;
    }

    private void verificarReconciliacion(SesionCaja sesion) {
        List<MovimientoCaja> movimientos = movimientoCajaRepository.findBySesion(sesion.getId());

        BigDecimal ingresosCalculados = BigDecimal.ZERO;
        BigDecimal egresosCalculados = BigDecimal.ZERO;
        for (MovimientoCaja m : movimientos) {
            BigDecimal efecto = m.esContraasiento() ? m.getMonto().negate() : m.getMonto();
            if (m.getTipo() == TipoMovimientoCaja.INGRESO) {
                ingresosCalculados = ingresosCalculados.add(efecto);
            } else {
                egresosCalculados = egresosCalculados.add(efecto);
            }
        }

        boolean ingresosCoinciden = ingresosCalculados.compareTo(sesion.getTotalIngresos()) == 0;
        boolean egresosCoinciden  = egresosCalculados.compareTo(sesion.getTotalEgresos()) == 0;
        if (ingresosCoinciden && egresosCoinciden) return;

        notificarAdmins("CAJA_ARQUEO_DISCREPANCIA", Map.of(
                "sede", nombreSede(sesion.getIdSede()),
                "diferencia", "ingresos calculados=" + ingresosCalculados
                        + " (registrado=" + sesion.getTotalIngresos() + ") | egresos calculados="
                        + egresosCalculados + " (registrado=" + sesion.getTotalEgresos() + ")"));

        log.warn("[CajaMonitoreoJob] Sesion de caja #{} desincronizada: ingresos {} vs {} | egresos {} vs {}",
                sesion.getId(), ingresosCalculados, sesion.getTotalIngresos(),
                egresosCalculados, sesion.getTotalEgresos());
    }

    private int umbralHoras() {
        return configuracionGlobalRepository.findByClave(CLAVE_UMBRAL_HORAS)
                .map(ConfiguracionGlobal::getValor)
                .map(valor -> {
                    try {
                        return Integer.parseInt(valor.trim());
                    } catch (NumberFormatException e) {
                        return UMBRAL_HORAS_DEFECTO;
                    }
                })
                .orElse(UMBRAL_HORAS_DEFECTO);
    }

    private void notificarAdmins(String tipoCodigo, Map<String, String> datosExtra) {
        for (UUID adminId : resolverAdministradoresPort.obtenerIdsAdministradoresActivos()) {
            crearNotificacionPort.notificar(CrearNotificacionCommand.builder()
                    .tipoCodigo(tipoCodigo)
                    .destinatarioUsuarioId(adminId)
                    .datosExtra(datosExtra)
                    .build());
        }
    }

    private String nombreUsuario(UUID usuarioId) {
        return perfilUsuarioRepository.buscarPorId(usuarioId).map(PerfilUsuario::getNombreCompleto).orElse("");
    }

    private String nombreSede(Long idSede) {
        return sedeRepository.findById(idSede).map(Sede::getNombre).orElse("Sede #" + idSede);
    }
}
