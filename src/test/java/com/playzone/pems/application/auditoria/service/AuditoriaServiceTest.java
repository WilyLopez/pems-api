package com.playzone.pems.application.auditoria.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import com.playzone.pems.domain.auditoria.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditoriaServiceTest {

    @Mock private LogAuditoriaRepository logRepository;

    private AuditoriaService service;

    @BeforeEach
    void setUp() {
        service = new AuditoriaService(logRepository, new ObjectMapper());
    }

    private RegistrarLogUseCase.Command comando() {
        return new RegistrarLogUseCase.Command(
                UUID.randomUUID(), AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_USUARIOS,
                "Staff", 1L, null, "nombre=Juan", "Perfil actualizado",
                "203.0.113.5", "Mozilla/5.0", AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO);
    }

    @Test
    void ejecutarSincrono_guardaInmediatamenteEnElRepositorio() {
        when(logRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.ejecutarSincrono(comando());

        ArgumentCaptor<LogAuditoria> captor = ArgumentCaptor.forClass(LogAuditoria.class);
        verify(logRepository).save(captor.capture());
        assertEquals("203.0.113.5", captor.getValue().getIpOrigen());
        assertEquals("Mozilla/5.0", captor.getValue().getUserAgent());
    }

    @Test
    void ejecutarSincrono_propagaLaExcepcionSiFallaElGuardado() {
        when(logRepository.save(any())).thenThrow(new RuntimeException("fallo de bd"));

        assertThrows(RuntimeException.class, () -> service.ejecutarSincrono(comando()));
    }
}
