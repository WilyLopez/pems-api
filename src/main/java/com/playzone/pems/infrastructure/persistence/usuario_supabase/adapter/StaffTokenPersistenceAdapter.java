package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.domain.usuario.model.StaffToken;
import com.playzone.pems.domain.usuario.repository.StaffTokenRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.StaffTokenEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.StaffTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StaffTokenPersistenceAdapter implements StaffTokenRepository {

    private final StaffTokenJpaRepository jpa;

    @Override
    public StaffToken guardar(StaffToken domain) {
        StaffTokenEntity entity = jpa.findById(domain.getId() != null ? domain.getId() : -1L)
                .orElse(new StaffTokenEntity());

        entity.setUsuarioId(domain.getUsuarioId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setTipo(domain.getTipo());
        entity.setExpiraAt(domain.getExpiraAt());
        entity.setUsadoAt(domain.getUsadoAt());

        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<StaffToken> buscarPorTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    private StaffToken toDomain(StaffTokenEntity e) {
        return StaffToken.builder()
                .id(e.getId())
                .usuarioId(e.getUsuarioId())
                .tokenHash(e.getTokenHash())
                .tipo(e.getTipo())
                .expiraAt(e.getExpiraAt())
                .usadoAt(e.getUsadoAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
