package com.playzone.pems.infrastructure.persistence.auditoria.adapter;

import com.playzone.pems.domain.auditoria.model.LogAuditoria;
import com.playzone.pems.domain.auditoria.repository.LogAuditoriaRepository;
import com.playzone.pems.infrastructure.persistence.auditoria.entity.LogAuditoriaEntity;
import com.playzone.pems.infrastructure.persistence.auditoria.jpa.LogAuditoriaJpaRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.PerfilUsuarioEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.PerfilUsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuditoriaPersistenceAdapter implements LogAuditoriaRepository {

    private static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final LogAuditoriaJpaRepository   logJpa;
    private final PerfilUsuarioJpaRepository  perfilUsuarioJpa;

    @Override
    @Transactional
    public LogAuditoria save(LogAuditoria log) {
        LogAuditoriaEntity entity = LogAuditoriaEntity.builder()
                .usuarioId(log.getIdUsuarioAdmin())
                .accion(log.getAccion())
                .modulo(log.getModulo())
                .entidadAfectada(log.getEntidadAfectada())
                .idEntidad(log.getIdEntidad())
                .valorAnterior(log.getValorAnterior())
                .valorNuevo(log.getValorNuevo())
                .descripcion(log.getDescripcion())
                .ipOrigen(log.getIpOrigen())
                .userAgent(log.getUserAgent())
                .nivel(log.getNivel() != null ? log.getNivel() : "INFO")
                .resultado(log.getResultado() != null ? log.getResultado() : "EXITOSO")
                .build();

        return toDomain(logJpa.save(entity), Map.of());
    }

    @Override
    public Optional<LogAuditoria> findById(Long id) {
        return logJpa.findById(id).map(e -> {
            Map<UUID, String> nombres = cargarNombres(List.of(e));
            return toDomain(e, nombres);
        });
    }

    @Override
    public Page<LogAuditoria> findByUsuario(UUID idUsuarioAdmin, Pageable pageable) {
        Page<LogAuditoriaEntity> page = logJpa.findByUsuarioIdOrderByFechaLogDesc(idUsuarioAdmin, pageable);
        Map<UUID, String> nombres = cargarNombres(page.getContent());
        return page.map(e -> toDomain(e, nombres));
    }

    @Override
    public Page<LogAuditoria> findByModuloAndEntidad(String modulo, String entidad, Pageable pageable) {
        Page<LogAuditoriaEntity> page = logJpa.findByModuloAndEntidadAfectadaOrderByFechaLogDesc(modulo, entidad, pageable);
        Map<UUID, String> nombres = cargarNombres(page.getContent());
        return page.map(e -> toDomain(e, nombres));
    }

    @Override
    public Page<LogAuditoria> findByEntidad(String entidadAfectada, Long idEntidad, Pageable pageable) {
        Page<LogAuditoriaEntity> page = logJpa.findByEntidadAfectadaAndIdEntidadOrderByFechaLogDesc(entidadAfectada, idEntidad, pageable);
        Map<UUID, String> nombres = cargarNombres(page.getContent());
        return page.map(e -> toDomain(e, nombres));
    }

    @Override
    public Page<LogAuditoria> findByFechasBetween(LocalDateTime desde, LocalDateTime hasta, Pageable pageable) {
        Page<LogAuditoriaEntity> page = logJpa.findByFechaLogBetweenOrderByFechaLogDesc(
                toOffsetDateTime(desde), toOffsetDateTime(hasta), pageable);
        Map<UUID, String> nombres = cargarNombres(page.getContent());
        return page.map(e -> toDomain(e, nombres));
    }

    @Override
    public Page<LogAuditoria> findByFiltros(LocalDateTime desde, LocalDateTime hasta,
                                             UUID idUsuario, String modulo, String accion,
                                             String entidad, String nivel, String resultado,
                                             Pageable pageable) {
        Page<LogAuditoriaEntity> page = logJpa.findByFiltros(
                toOffsetDateTime(desde), toOffsetDateTime(hasta),
                idUsuario, modulo, accion, entidad, nivel, resultado,
                pageable);
        Map<UUID, String> nombres = cargarNombres(page.getContent());
        return page.map(e -> toDomain(e, nombres));
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime ldt) {
        return ldt.atZone(ZONA_LIMA).toOffsetDateTime();
    }

    private Map<UUID, String> cargarNombres(Collection<LogAuditoriaEntity> entities) {
        Set<UUID> ids = entities.stream()
                .map(LogAuditoriaEntity::getUsuarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return perfilUsuarioJpa.findAllById(ids).stream()
                .collect(Collectors.toMap(PerfilUsuarioEntity::getId, PerfilUsuarioEntity::getNombreCompleto));
    }

    private LogAuditoria toDomain(LogAuditoriaEntity e, Map<UUID, String> nombres) {
        return LogAuditoria.builder()
                .id(e.getId())
                .idUsuarioAdmin(e.getUsuarioId())
                .nombreUsuario(e.getUsuarioId() != null ? nombres.get(e.getUsuarioId()) : null)
                .accion(e.getAccion())
                .modulo(e.getModulo())
                .entidadAfectada(e.getEntidadAfectada())
                .idEntidad(e.getIdEntidad())
                .valorAnterior(e.getValorAnterior())
                .valorNuevo(e.getValorNuevo())
                .descripcion(e.getDescripcion())
                .ipOrigen(e.getIpOrigen())
                .userAgent(e.getUserAgent())
                .nivel(e.getNivel())
                .resultado(e.getResultado())
                .fechaLog(e.getFechaLog())
                .build();
    }
}
