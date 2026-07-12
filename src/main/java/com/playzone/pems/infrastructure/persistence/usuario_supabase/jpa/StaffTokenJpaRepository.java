package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.StaffTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StaffTokenJpaRepository extends JpaRepository<StaffTokenEntity, Long> {

    Optional<StaffTokenEntity> findByTokenHash(String tokenHash);
}
