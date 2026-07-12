package com.playzone.pems.infrastructure.persistence.usuario_supabase.adapter;

import com.playzone.pems.domain.usuario.model.ClienteToken;
import com.playzone.pems.domain.usuario.repository.ClienteTokenRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.ClienteTokenEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.ClienteTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ClienteTokenPersistenceAdapter implements ClienteTokenRepository {

    private final ClienteTokenJpaRepository jpa;

    @Override
    public ClienteToken guardar(ClienteToken domain) {
        ClienteTokenEntity entity = jpa.findById(domain.getId() != null ? domain.getId() : -1L)
                .orElse(new ClienteTokenEntity());

        entity.setClienteId(domain.getClienteId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setTipo(domain.getTipo());
        entity.setMetadata(domain.getMetadata() != null ? domain.getMetadata() : "{}");
        entity.setExpiraAt(domain.getExpiraAt());
        entity.setUsadoAt(domain.getUsadoAt());

        return toDomain(jpa.save(entity));
    }

    @Override
    public Optional<ClienteToken> buscarPorTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    private ClienteToken toDomain(ClienteTokenEntity e) {
        return ClienteToken.builder()
                .id(e.getId())
                .clienteId(e.getClienteId())
                .tokenHash(e.getTokenHash())
                .tipo(e.getTipo())
                .metadata(e.getMetadata())
                .expiraAt(e.getExpiraAt())
                .usadoAt(e.getUsadoAt())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
