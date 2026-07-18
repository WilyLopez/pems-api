package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.domain.usuario.model.StaffPerfil;
import com.playzone.pems.domain.usuario.repository.StaffPerfilRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.StaffPerfilEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.StaffPerfilJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StaffPerfilPersistenceAdapter implements StaffPerfilRepository {

    private final StaffPerfilJpaRepository jpa;

    @Override
    public Optional<StaffPerfil> buscarPorId(Long id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<StaffPerfil> buscarPorUsuarioId(UUID usuarioId) {
        return jpa.findByUsuarioId(usuarioId).map(this::toDomain);
    }

    @Override
    public Optional<StaffPerfil> buscarPorCorreo(String correo) {
        return jpa.findByCorreo(correo).map(this::toDomain);
    }

    @Override
    public java.util.List<StaffPerfil> listarTodos() {
        return jpa.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public StaffPerfil guardar(StaffPerfil domain) {
        StaffPerfilEntity entity = jpa.findById(domain.getId() != null ? domain.getId() : -1L)
                .orElse(new StaffPerfilEntity());

        entity.setUsuarioId(domain.getUsuarioId());
        entity.setSedeId(domain.getSedeId());
        entity.setCodigoEmpleado(domain.getCodigoEmpleado());
        entity.setFechaIngreso(domain.getFechaIngreso());
        entity.setTelefonoEmergencia(domain.getTelefonoEmergencia());
        entity.setObservaciones(domain.getObservaciones());
        entity.setEsActivo(domain.isEsActivo());
        entity.setDebeCambiarContrasena(domain.isDebeCambiarContrasena());
        entity.setIntentosFallidos(domain.getIntentosFallidos() != null ? domain.getIntentosFallidos() : 0);
        entity.setBloqueadoHasta(domain.getBloqueadoHasta());
        entity.setCreatedBy(domain.getCreatedBy());
        entity.setUpdatedBy(domain.getUpdatedBy());

        return toDomain(jpa.save(entity));
    }

    @Override
    public long contarActivosPorRol(String rolCodigo) {
        return jpa.contarActivosPorRol(rolCodigo);
    }

    @Override
    public long contarActivosPorRolExcluyendo(String rolCodigo, Long excludeId) {
        return jpa.contarActivosPorRolExcluyendo(rolCodigo, excludeId);
    }

    private StaffPerfil toDomain(StaffPerfilEntity e) {
        return StaffPerfil.builder()
                .id(e.getId())
                .usuarioId(e.getUsuarioId())
                .sedeId(e.getSedeId())
                .codigoEmpleado(e.getCodigoEmpleado())
                .fechaIngreso(e.getFechaIngreso())
                .telefonoEmergencia(e.getTelefonoEmergencia())
                .observaciones(e.getObservaciones())
                .esActivo(e.isEsActivo())
                .debeCambiarContrasena(e.isDebeCambiarContrasena())
                .intentosFallidos(e.getIntentosFallidos())
                .bloqueadoHasta(e.getBloqueadoHasta())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .createdBy(e.getCreatedBy())
                .updatedBy(e.getUpdatedBy())
                .build();
    }
}
