package com.playzone.pems.application.auditoria.port.in;

import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ObtenerAuditoriaUseCase {

    record FiltrosQuery(
            LocalDateTime desde,
            LocalDateTime hasta,
            UUID   idUsuario,
            String modulo,
            String accion,
            String entidad,
            String nivel,
            String resultado,
            int    pagina,
            int    tamano
    ) {}

    Page<LogAuditoria> listarPorFiltros(FiltrosQuery filtros);

    LogAuditoria obtenerPorId(Long id);

    Page<LogAuditoria> listarPorUsuario(UUID idUsuario, int pagina, int tamano);

    Page<LogAuditoria> listarPorEntidad(String entidadAfectada, Long idEntidad, int pagina, int tamano);
}
