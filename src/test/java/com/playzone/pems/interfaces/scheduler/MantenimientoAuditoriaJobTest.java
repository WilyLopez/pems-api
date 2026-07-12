package com.playzone.pems.interfaces.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MantenimientoAuditoriaJobTest {

    @Mock private JdbcTemplate jdbc;

    private MantenimientoAuditoriaJob job;

    @BeforeEach
    void setUp() {
        job = new MantenimientoAuditoriaJob(jdbc);
    }

    @Test
    void testEjecutarMantenimientoDiarioInvocaLaFuncionSql() {
        when(jdbc.queryForList("SELECT * FROM app.mantenimiento_diario()")).thenReturn(List.of(
                Map.of("tarea", "particion_auditoria_creada", "registros_afectados", 1),
                Map.of("tarea", "envio_email_antiguos_eliminados", "registros_afectados", 12)
        ));

        job.ejecutarMantenimientoDiario();

        verify(jdbc).queryForList("SELECT * FROM app.mantenimiento_diario()");
    }

    @Test
    void testEjecutarMantenimientoDiarioNoPropagaExcepcion() {
        when(jdbc.queryForList(anyString())).thenThrow(new RuntimeException("conexion caida"));

        assertDoesNotThrow(() -> job.ejecutarMantenimientoDiario());
    }
}
