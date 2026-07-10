package com.playzone.pems.infrastructure.persistence.finanzas.jpa;

import com.playzone.pems.infrastructure.persistence.finanzas.entity.ArqueoCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArqueoCajaJpaRepository extends JpaRepository<ArqueoCajaEntity, Long> {

    List<ArqueoCajaEntity> findBySesionCaja_IdOrderByFechaCreacionAsc(Long idSesionCaja);
}
