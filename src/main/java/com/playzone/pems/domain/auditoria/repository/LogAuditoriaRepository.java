package com.playzone.pems.domain.auditoria.repository;

import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LogAuditoriaRepository {

    LogAuditoria save(LogAuditoria log);

    Optional<LogAuditoria> findById(Long id);

    Page<LogAuditoria> findByUsuario(UUID idUsuarioAdmin, Pageable pageable);

    Page<LogAuditoria> findByModuloAndEntidad(
            String modulo, String entidad, Pageable pageable);

    Page<LogAuditoria> findByEntidad(
            String entidadAfectada, Long idEntidad, Pageable pageable);

    Page<LogAuditoria> findByFechasBetween(
            LocalDateTime desde, LocalDateTime hasta, Pageable pageable);

    Page<LogAuditoria> findByFiltros(
            LocalDateTime desde, LocalDateTime hasta,
            UUID idUsuario, String modulo, String accion,
            String entidad, String nivel, String resultado,
            Pageable pageable);
}
