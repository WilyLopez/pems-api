package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.MovimientoCaja;

import java.util.List;
import java.util.Optional;

public interface MovimientoCajaRepository {
    List<MovimientoCaja> findBySesion(Long idSesionCaja);
    Optional<MovimientoCaja> findById(Long id);
    boolean existsContraasientoPara(Long idMovimientoOriginal);
    MovimientoCaja save(MovimientoCaja movimiento);
    void deleteById(Long id);
}
