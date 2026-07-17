package com.playzone.pems.domain.comercial.repository;

import com.playzone.pems.domain.comercial.model.CategoriaServicio;

import java.util.List;
import java.util.Optional;

public interface CategoriaServicioRepository {
    List<CategoriaServicio> findAllActivas();
    List<CategoriaServicio> findAll();
    Optional<CategoriaServicio> findById(Long id);
    CategoriaServicio save(CategoriaServicio categoria);
    void deleteById(Long id);
    boolean existsByNombre(String nombre);
    boolean existsByNombreExcludingId(String nombre, Long id);
    boolean tieneServiciosAsociados(Long id);
}
