package com.playzone.pems.domain.usuario.repository;

import com.playzone.pems.domain.usuario.model.PerfilUsuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerfilUsuarioRepository {

    Optional<PerfilUsuario> buscarPorId(UUID id);

    List<PerfilUsuario> buscarPorIds(List<UUID> ids);

    Optional<PerfilUsuario> buscarPorCorreo(String correo);

    void actualizarPerfil(UUID id, String nombre, String telefono);
}
