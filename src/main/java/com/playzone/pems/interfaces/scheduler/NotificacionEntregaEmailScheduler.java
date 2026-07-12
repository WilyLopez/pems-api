package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.application.notificacion.dto.command.CrearNotificacionCommand;
import com.playzone.pems.application.notificacion.port.out.CrearNotificacionPort;
import com.playzone.pems.application.notificacion.port.out.ResolverAdministradoresPort;
import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.notificacion.model.NotificacionEntrega;
import com.playzone.pems.domain.notificacion.repository.NotificacionEntregaRepository;
import com.playzone.pems.domain.notificacion.repository.NotificacionRepository;
import com.playzone.pems.infrastructure.external.correo.JavaMailCorreoClient;
import com.playzone.pems.infrastructure.external.correo.renderizador.ContenidoCorreo;
import com.playzone.pems.infrastructure.external.correo.renderizador.RenderizadorCorreoRegistry;
import com.playzone.pems.infrastructure.external.correo.renderizador.RenderizadorCorreoTransaccional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificacionEntregaEmailScheduler {

    private static final int LOTE = 50;
    private static final int MAX_INTENTOS = 3;
    private static final String TIPO_DESCONOCIDO = "desconocido";

    private final NotificacionEntregaRepository entregaRepo;
    private final NotificacionRepository notificacionRepo;
    private final RenderizadorCorreoRegistry renderizadores;
    private final JavaMailCorreoClient correoClient;
    private final CrearNotificacionPort notificacionPort;
    private final ResolverAdministradoresPort resolverAdministradoresPort;
    private final MeterRegistry meterRegistry;

    @Value("${playzone.correo.alerta.tasa-fallo-umbral:0.2}")
    private double umbralTasaFallo;

    @Value("${playzone.correo.alerta.minimo-muestra:5}")
    private int minimoMuestraAlerta;

    @Scheduled(fixedDelay = 30_000, initialDelay = 20_000)
    @Transactional
    public void procesarPendientes() {
        procesarLote(entregaRepo.findPendientesEmail(LOTE));
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "America/Lima")
    @Transactional
    public void reintentarFallidos() {
        procesarLote(entregaRepo.findParaReintentarEmail(MAX_INTENTOS, LOTE));
    }

    private void procesarLote(List<NotificacionEntrega> entregas) {
        Timer.Sample muestra = Timer.start(meterRegistry);
        int enviados = 0;
        int fallidos = 0;

        for (NotificacionEntrega entrega : entregas) {
            if (procesarEntrega(entrega)) {
                enviados++;
            } else {
                fallidos++;
            }
        }

        muestra.stop(meterRegistry.timer("notificacion.email.lote.duracion"));
        alertarSiTasaFalloAlta(enviados, fallidos);
    }

    private boolean procesarEntrega(NotificacionEntrega entrega) {
        Optional<Notificacion> notificacionOpt = notificacionRepo.findById(entrega.getNotificacionId());
        if (notificacionOpt.isEmpty()) {
            marcarNoRecuperable(entrega, "La notificación asociada ya no existe", TIPO_DESCONOCIDO);
            return false;
        }
        Notificacion notificacion = notificacionOpt.get();
        String tipoCodigo = notificacion.getTipoCodigo();

        Optional<RenderizadorCorreoTransaccional> renderizadorOpt = renderizadores.resolver(tipoCodigo);
        if (renderizadorOpt.isEmpty()) {
            marcarNoRecuperable(entrega, "Sin renderizador configurado para el tipo " + tipoCodigo, tipoCodigo);
            return false;
        }

        try {
            ContenidoCorreo contenido = renderizadorOpt.get().renderizar(notificacion);
            if (contenido.getDestinatario() == null || contenido.getDestinatario().isBlank()) {
                marcarNoRecuperable(entrega, "El destinatario no tiene correo registrado", tipoCodigo);
                return false;
            }

            if (contenido.getAdjuntos() == null || contenido.getAdjuntos().isEmpty()) {
                correoClient.enviar(contenido.getDestinatario(), contenido.getAsunto(), contenido.getCuerpoHtml());
            } else {
                correoClient.enviarConAdjuntos(
                        contenido.getDestinatario(), contenido.getAsunto(), contenido.getCuerpoHtml(), contenido.getAdjuntos());
            }

            entrega.setEstado("ENVIADO");
            entrega.setIntentos(entrega.getIntentos() + 1);
            entrega.setEnviadoAt(OffsetDateTime.now());
            entrega.setMensajeError(null);
            entregaRepo.save(entrega);
            meterRegistry.counter("notificacion.email.enviados", "tipo", tipoCodigo).increment();
            return true;
        } catch (Exception e) {
            log.warn("Error al enviar correo transaccional (entrega {}): {}", entrega.getId(), e.getMessage());
            marcarFallo(entrega, e.getMessage(), tipoCodigo);
            return false;
        }
    }

    private void marcarFallo(NotificacionEntrega entrega, String mensajeError, String tipoCodigo) {
        int intentos = entrega.getIntentos() + 1;
        boolean agotado = intentos >= MAX_INTENTOS;
        entrega.setIntentos(intentos);
        entrega.setMensajeError(mensajeError);
        entrega.setEstado(agotado ? "REBOTADO" : "ERROR");
        entregaRepo.save(entrega);
        meterRegistry.counter("notificacion.email.fallidos", "tipo", tipoCodigo).increment();

        if (agotado) {
            alertarAdministradores(entrega, mensajeError);
        }
    }

    private void marcarNoRecuperable(NotificacionEntrega entrega, String mensajeError, String tipoCodigo) {
        entrega.setIntentos(entrega.getIntentos() + 1);
        entrega.setMensajeError(mensajeError);
        entrega.setEstado("REBOTADO");
        entregaRepo.save(entrega);
        meterRegistry.counter("notificacion.email.fallidos", "tipo", tipoCodigo).increment();
        alertarAdministradores(entrega, mensajeError);
    }

    private void alertarSiTasaFalloAlta(int enviados, int fallidos) {
        int total = enviados + fallidos;
        if (total < minimoMuestraAlerta) return;

        double tasaFallo = (double) fallidos / total;
        if (tasaFallo > umbralTasaFallo) {
            log.warn("Tasa de fallo de envío de correo transaccional elevada: {}% ({} de {} en este lote, umbral {}%)",
                    Math.round(tasaFallo * 100), fallidos, total, Math.round(umbralTasaFallo * 100));
        }
    }

    private void alertarAdministradores(NotificacionEntrega entrega, String mensajeError) {
        for (UUID idAdmin : resolverAdministradoresPort.obtenerIdsAdministradoresActivos()) {
            notificacionPort.notificar(CrearNotificacionCommand.builder()
                    .tipoCodigo("ERROR_ENVIO_EMAIL")
                    .destinatarioUsuarioId(idAdmin)
                    .entidadTipo("notificacion_entrega")
                    .entidadId(entrega.getId())
                    .datosExtra(Map.of(
                            "destinatario", "notificación #" + entrega.getNotificacionId(),
                            "error", mensajeError != null ? mensajeError : "desconocido"))
                    .build());
        }
    }
}
