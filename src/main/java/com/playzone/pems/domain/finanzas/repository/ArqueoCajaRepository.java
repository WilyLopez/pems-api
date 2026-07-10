package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.ArqueoCaja;

import java.util.List;

public interface ArqueoCajaRepository {
    ArqueoCaja save(ArqueoCaja arqueo);
    List<ArqueoCaja> findBySesion(Long idSesionCaja);
}
