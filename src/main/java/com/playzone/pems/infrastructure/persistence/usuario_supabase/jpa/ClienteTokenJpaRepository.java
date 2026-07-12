package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.ClienteTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteTokenJpaRepository extends JpaRepository<ClienteTokenEntity, Long> {

    Optional<ClienteTokenEntity> findByTokenHash(String tokenHash);
}
