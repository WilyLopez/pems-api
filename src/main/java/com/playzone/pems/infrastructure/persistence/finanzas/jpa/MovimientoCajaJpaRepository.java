package com.playzone.pems.infrastructure.persistence.finanzas.jpa;

import com.playzone.pems.infrastructure.persistence.finanzas.entity.MovimientoCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoCajaJpaRepository extends JpaRepository<MovimientoCajaEntity, Long> {

    List<MovimientoCajaEntity> findBySesionCaja_IdOrderByFechaCreacionAsc(Long idSesionCaja);

    boolean existsByMovimientoAnuladoId(Long movimientoAnuladoId);
}
