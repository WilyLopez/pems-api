package com.playzone.pems.infrastructure.persistence.usuario.jpa;

import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SedeJpaRepository extends JpaRepository<SedeEntity, Long> {

    List<SedeEntity> findByDeletedAtIsNull();

    Optional<SedeEntity> findFirstByDeletedAtIsNullOrderByIdAsc();

    boolean existsByRuc(String ruc);
}