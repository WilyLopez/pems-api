package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.domain.usuario.repository.UsuarioRolRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.UsuarioRolJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UsuarioRolPersistenceAdapter implements UsuarioRolRepository {

    private final UsuarioRolJpaRepository jpa;

    @Override
    public List<String> listarCodigosRolPorUsuario(UUID usuarioId) {
        return jpa.findRolCodigosByUsuarioId(usuarioId);
    }

    @Override
    public Map<UUID, List<String>> listarCodigosRolPorUsuarios(List<UUID> usuarioIds) {
        return jpa.findRolCodigosByUsuarioIds(usuarioIds).stream()
                .collect(Collectors.groupingBy(
                        UsuarioRolJpaRepository.RolPorUsuarioProjection::getUsuarioId,
                        Collectors.mapping(UsuarioRolJpaRepository.RolPorUsuarioProjection::getRolCodigo, Collectors.toList())
                ));
    }

    @Override
    public List<String> listarCodigosPermisoPorUsuario(UUID usuarioId) {
        return jpa.findPermisoCodigosByUsuarioId(usuarioId);
    }

    @Override
    public void guardar(com.playzone.pems.domain.usuario.model.UsuarioRol domain) {
        com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.UsuarioRolEntity entity =
                new com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.UsuarioRolEntity();
        entity.setUsuarioId(domain.getUsuarioId());
        entity.setRolCodigo(domain.getRolCodigo());
        entity.setAsignadoAt(java.time.OffsetDateTime.now());
        jpa.save(entity);
    }

    @Override
    public void eliminar(UUID usuarioId, String rolCodigo) {
        jpa.deleteByUsuarioIdAndRolCodigo(usuarioId, rolCodigo);
    }
}
