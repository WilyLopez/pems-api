package com.playzone.pems.domain.usuario.repository;

import com.playzone.pems.domain.usuario.model.UsuarioRol;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface UsuarioRolRepository {

    List<String> listarCodigosRolPorUsuario(UUID usuarioId);

    Map<UUID, List<String>> listarCodigosRolPorUsuarios(List<UUID> usuarioIds);

    List<String> listarCodigosPermisoPorUsuario(UUID usuarioId);

    void guardar(UsuarioRol usuarioRol);

    void eliminar(UUID usuarioId, String rolCodigo);
}
