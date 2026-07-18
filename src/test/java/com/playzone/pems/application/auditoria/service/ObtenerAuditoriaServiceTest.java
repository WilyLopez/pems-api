package com.playzone.pems.application.auditoria.service;

import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import com.playzone.pems.domain.auditoria.repository.LogAuditoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObtenerAuditoriaServiceTest {

    @Mock private LogAuditoriaRepository logRepository;

    private ObtenerAuditoriaService service;

    @BeforeEach
    void setUp() {
        service = new ObtenerAuditoriaService(logRepository);
    }

    @Test
    void listarPorEntidad_delegaAlRepositorioConLaEntidadYElId() {
        LogAuditoria log = LogAuditoria.builder()
                .id(1L).entidadAfectada("Staff").idEntidad(7L).accion("DESACTIVAR").build();
        Page<LogAuditoria> pagina = new PageImpl<>(java.util.List.of(log));
        when(logRepository.findByEntidad(eq("Staff"), eq(7L), any(Pageable.class))).thenReturn(pagina);

        Page<LogAuditoria> resultado = service.listarPorEntidad("Staff", 7L, 0, 20);

        assertEquals(1, resultado.getTotalElements());
        verify(logRepository).findByEntidad(eq("Staff"), eq(7L), any(Pageable.class));
    }
}
