package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.domain.marketing.model.CampanaEmail;
import com.playzone.pems.domain.marketing.model.EnvioEmail;
import com.playzone.pems.domain.marketing.repository.CampanaEmailRepository;
import com.playzone.pems.domain.marketing.repository.EnvioEmailRepository;
import com.playzone.pems.infrastructure.external.correo.JavaMailCorreoClient;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioEmailScheduler {

    private static final int LOTE         = 50;
    private static final int MAX_INTENTOS = 3;

    private final CampanaEmailRepository campanaRepo;
    private final EnvioEmailRepository   envioRepo;
    private final JavaMailCorreoClient   correoClient;
    private final MeterRegistry          meterRegistry;

    @Value("${playzone.correo.alerta.tasa-fallo-umbral:0.2}")
    private double umbralTasaFallo;

    @Value("${playzone.correo.alerta.minimo-muestra:5}")
    private int minimoMuestraAlerta;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void procesarEnviosPendientes() {
        Timer.Sample muestra = Timer.start(meterRegistry);
        int totalEnviados = 0;
        int totalFallidos = 0;

        List<CampanaEmail> campanas = campanaRepo.findProgramadasParaEnviar();

        for (CampanaEmail campana : campanas) {
            campanaRepo.actualizarEstado(campana.getId(), "ENVIANDO");
            ResultadoLote resultado = procesarLote(campana.getId());
            totalEnviados += resultado.enviados();
            totalFallidos += resultado.fallidos();
        }

        List<CampanaEmail> enviando = campanaRepo.findByEstado("ENVIANDO");

        for (CampanaEmail campana : enviando) {
            ResultadoLote resultado = procesarLote(campana.getId());
            totalEnviados += resultado.enviados();
            totalFallidos += resultado.fallidos();

            long pendientes = envioRepo.countByCampanaAndEstado(campana.getId(), "PENDIENTE");
            if (pendientes == 0) {
                campanaRepo.actualizarEstado(campana.getId(), "FINALIZADA");
                log.info("Campaña {} finalizada.", campana.getId());
            }
        }

        muestra.stop(meterRegistry.timer("marketing.email.lote.duracion"));
        alertarSiTasaFalloAlta(totalEnviados, totalFallidos);
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "America/Lima")
    @Transactional
    public void reintentarFallidos() {
        List<EnvioEmail> fallidos = envioRepo.findParaReintentar(MAX_INTENTOS);
        for (EnvioEmail envio : fallidos) {
            enviarCorreo(envio);
        }
    }

    private record ResultadoLote(int enviados, int fallidos) {}

    private ResultadoLote procesarLote(Long idCampana) {
        List<EnvioEmail> pendientes = envioRepo.findPendientesByCampana(idCampana, LOTE);
        int enviados  = 0;
        int fallidos  = 0;

        for (EnvioEmail envio : pendientes) {
            if (enviarCorreo(envio)) {
                enviados++;
            } else {
                fallidos++;
            }
        }

        if (enviados > 0) campanaRepo.incrementarEnviados(idCampana, enviados);
        if (fallidos > 0) campanaRepo.incrementarFallidos(idCampana, fallidos);
        return new ResultadoLote(enviados, fallidos);
    }

    private boolean enviarCorreo(EnvioEmail envio) {
        try {
            String cuerpo = envio.getCuerpoHtml() != null ? envio.getCuerpoHtml() : "<p>" + envio.getAsunto() + "</p>";
            correoClient.enviar(envio.getDestinatario(), envio.getAsunto(), cuerpo);
            envioRepo.save(envio.toBuilder()
                    .estado("ENVIADO")
                    .intentos(envio.getIntentos() + 1)
                    .fechaEnvio(Instant.now())
                    .mensajeError(null)
                    .build());
            meterRegistry.counter("marketing.email.enviados").increment();
            return true;
        } catch (Exception e) {
            log.warn("Error al enviar correo a {}: {}", envio.getDestinatario(), e.getMessage());
            String nuevoEstado = (envio.getIntentos() + 1 >= MAX_INTENTOS) ? "REBOTADO" : "ERROR";
            envioRepo.save(envio.toBuilder()
                    .estado(nuevoEstado)
                    .intentos(envio.getIntentos() + 1)
                    .mensajeError(e.getMessage())
                    .build());
            meterRegistry.counter("marketing.email.fallidos").increment();
            return false;
        }
    }

    private void alertarSiTasaFalloAlta(int enviados, int fallidos) {
        int total = enviados + fallidos;
        if (total < minimoMuestraAlerta) return;

        double tasaFallo = (double) fallidos / total;
        if (tasaFallo > umbralTasaFallo) {
            log.warn("Tasa de fallo de envío de correo de marketing elevada: {}% ({} de {} en este lote, umbral {}%)",
                    Math.round(tasaFallo * 100), fallidos, total, Math.round(umbralTasaFallo * 100));
        }
    }
}
