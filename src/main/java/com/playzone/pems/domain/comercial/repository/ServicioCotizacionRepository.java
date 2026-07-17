package com.playzone.pems.domain.comercial.repository;

import com.playzone.pems.domain.comercial.model.ServicioCotizacion;

import java.util.List;
import java.util.Optional;

public interface ServicioCotizacionRepository {
    List<ServicioCotizacion> findAllActivos();
    List<ServicioCotizacion> findAll();
    Optional<ServicioCotizacion> findById(Long id);
    ServicioCotizacion save(ServicioCotizacion servicio);
    void deleteById(Long id);
    boolean existsByNombre(String nombre);
    boolean existsByNombreExcludingId(String nombre, Long id);
}
