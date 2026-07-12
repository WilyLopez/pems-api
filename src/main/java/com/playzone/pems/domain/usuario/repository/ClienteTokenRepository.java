package com.playzone.pems.domain.usuario.repository;

import com.playzone.pems.domain.usuario.model.ClienteToken;

import java.util.Optional;

public interface ClienteTokenRepository {

    ClienteToken guardar(ClienteToken token);

    Optional<ClienteToken> buscarPorTokenHash(String tokenHash);
}
