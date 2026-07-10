package com.playzone.pems.infrastructure.persistence.preferencia.adapter;

import com.playzone.pems.domain.preferencia.model.PreferenciaUsuario;
import com.playzone.pems.domain.preferencia.repository.PreferenciaUsuarioRepository;
import com.playzone.pems.infrastructure.persistence.preferencia.entity.PreferenciaUsuarioEntity;
import com.playzone.pems.infrastructure.persistence.preferencia.jpa.PreferenciaUsuarioJpaRepository;
import com.playzone.pems.infrastructure.persistence.preferencia.mapper.PreferenciaUsuarioMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PreferenciaUsuarioPersistenceAdapter implements PreferenciaUsuarioRepository {

    private final PreferenciaUsuarioJpaRepository jpaRepository;
    private final PreferenciaUsuarioMapper        mapper;

    @Override
    public Optional<PreferenciaUsuario> buscarPorUsuarioId(UUID usuarioId) {
        return jpaRepository.findById(usuarioId).map(mapper::toDomain);
    }

    @Override
    public PreferenciaUsuario guardar(PreferenciaUsuario preferencia) {
        PreferenciaUsuarioEntity entity = jpaRepository.findById(preferencia.getUsuarioId())
                .orElseGet(() -> PreferenciaUsuarioEntity.builder()
                        .usuarioId(preferencia.getUsuarioId())
                        .build());

        PreferenciaUsuarioEntity updated = mapper.toEntity(preferencia);
        entity.setTema(updated.getTema());
        entity.setPreferenciasExtras(updated.getPreferenciasExtras());

        return mapper.toDomain(jpaRepository.save(entity));
    }
}
