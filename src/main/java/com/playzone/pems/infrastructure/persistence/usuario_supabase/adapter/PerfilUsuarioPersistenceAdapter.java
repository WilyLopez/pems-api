package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.domain.usuario.model.PerfilUsuario;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.PerfilUsuarioEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.PerfilUsuarioJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PerfilUsuarioPersistenceAdapter implements PerfilUsuarioRepository {

    private final PerfilUsuarioJpaRepository jpa;

    @Override
    public Optional<PerfilUsuario> buscarPorId(UUID id) {
        return jpa.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public List<PerfilUsuario> buscarPorIds(List<UUID> ids) {
        return jpa.findByIdInAndDeletedAtIsNull(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<PerfilUsuario> buscarPorCorreo(String correo) {
        return jpa.findByCorreoAndDeletedAtIsNull(correo).map(this::toDomain);
    }

    @Override
    @Transactional
    public void actualizarPerfil(UUID id, String nombre, String telefono) {
        jpa.actualizarNombreYTelefono(id, nombre, telefono);
    }

    private PerfilUsuario toDomain(PerfilUsuarioEntity e) {
        return PerfilUsuario.builder()
                .id(e.getId())
                .nombreCompleto(e.getNombreCompleto())
                .correo(e.getCorreo())
                .telefono(e.getTelefono())
                .fotoPerfilPath(e.getFotoPerfilPath())
                .ultimoLoginAt(e.getUltimoLoginAt())
                .creadoEn(e.getCreatedAt())
                .build();
    }
}
