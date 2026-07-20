package com.playzone.pems.infrastructure.persistence.calendario.jpa;

import com.playzone.pems.infrastructure.persistence.calendario.entity.TurnoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TurnoJpaRepository extends JpaRepository<TurnoEntity, String> {

    Optional<TurnoEntity> findByCodigo(String codigo);

    @Query("SELECT t FROM TurnoEntity t WHERE t.id = :idNumerico")
    Optional<TurnoEntity> buscarPorIdNumerico(@Param("idNumerico") Long idNumerico);
}