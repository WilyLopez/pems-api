package com.playzone.pems.domain.usuario.repository;

import com.playzone.pems.domain.usuario.model.StaffToken;

import java.util.Optional;

public interface StaffTokenRepository {

    StaffToken guardar(StaffToken token);

    Optional<StaffToken> buscarPorTokenHash(String tokenHash);
}
