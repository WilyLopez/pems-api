package com.playzone.pems.interfaces.scheduler;

import com.playzone.pems.domain.marketing.model.CampanaEmail;
import com.playzone.pems.domain.marketing.model.EnvioEmail;
import com.playzone.pems.domain.marketing.repository.CampanaEmailRepository;
import com.playzone.pems.domain.marketing.repository.EnvioEmailRepository;
import com.playzone.pems.infrastructure.external.correo.JavaMailCorreoClient;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvioEmailSchedulerTest {

    @Mock private CampanaEmailRepository campanaRepo;
    @Mock private EnvioEmailRepository envioRepo;
    @Mock private JavaMailCorreoClient correoClient;
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private EnvioEmailScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EnvioEmailScheduler(campanaRepo, envioRepo, correoClient, meterRegistry);
    }

    @Test
    void testEnviaConCuerpoHtmlRealCuandoEstaPresente() {
        EnvioEmail envio = EnvioEmail.builder()
                .id(1L).destinatario("cliente@correo.com").asunto("Promo")
                .cuerpoHtml("<p>contenido real de la campaña</p>")
                .estado("PENDIENTE").intentos(0).build();

        when(campanaRepo.findProgramadasParaEnviar()).thenReturn(List.of());
        when(campanaRepo.findByEstado("ENVIANDO")).thenReturn(
                List.of(CampanaEmail.builder().id(1L).estado("ENVIANDO").build()));
        when(envioRepo.findPendientesByCampana(1L, 50)).thenReturn(List.of(envio));
        when(envioRepo.countByCampanaAndEstado(1L, "PENDIENTE")).thenReturn(0L);

        scheduler.procesarEnviosPendientes();

        ArgumentCaptor<String> cuerpoCaptor = ArgumentCaptor.forClass(String.class);
        verify(correoClient).enviar(eq("cliente@correo.com"), eq("Promo"), cuerpoCaptor.capture());
        assertEquals("<p>contenido real de la campaña</p>", cuerpoCaptor.getValue());
        assertEquals(1.0, meterRegistry.counter("marketing.email.enviados").count());
        assertNotNull(meterRegistry.find("marketing.email.lote.duracion").timer());
    }

    @Test
    void testUsaFallbackSiCuerpoHtmlEsNulo() {
        EnvioEmail envio = EnvioEmail.builder()
                .id(2L).destinatario("legacy@correo.com").asunto("Asunto legacy")
                .cuerpoHtml(null)
                .estado("PENDIENTE").intentos(0).build();

        when(campanaRepo.findProgramadasParaEnviar()).thenReturn(List.of());
        when(campanaRepo.findByEstado("ENVIANDO")).thenReturn(
                List.of(CampanaEmail.builder().id(2L).estado("ENVIANDO").build()));
        when(envioRepo.findPendientesByCampana(2L, 50)).thenReturn(List.of(envio));
        when(envioRepo.countByCampanaAndEstado(2L, "PENDIENTE")).thenReturn(0L);

        scheduler.procesarEnviosPendientes();

        verify(correoClient).enviar("legacy@correo.com", "Asunto legacy", "<p>Asunto legacy</p>");
    }

    @Test
    void testFinalizaCampanaCuandoNoQuedanPendientes() {
        when(campanaRepo.findProgramadasParaEnviar()).thenReturn(List.of());
        when(campanaRepo.findByEstado("ENVIANDO")).thenReturn(
                List.of(CampanaEmail.builder().id(3L).estado("ENVIANDO").build()));
        when(envioRepo.findPendientesByCampana(3L, 50)).thenReturn(List.of());
        when(envioRepo.countByCampanaAndEstado(3L, "PENDIENTE")).thenReturn(0L);

        scheduler.procesarEnviosPendientes();

        verify(campanaRepo).actualizarEstado(3L, "FINALIZADA");
    }
}
