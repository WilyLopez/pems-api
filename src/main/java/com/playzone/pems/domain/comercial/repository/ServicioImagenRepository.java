package com.playzone.pems.domain.comercial.repository;

import com.playzone.pems.domain.comercial.model.ServicioImagen;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServicioImagenRepository {
    List<ServicioImagen> findByServicio(Long idServicio);
    Map<Long, List<ServicioImagen>> findByServicios(List<Long> idsServicio);
    Optional<ServicioImagen> findById(Long id);
    ServicioImagen save(ServicioImagen imagen);
    void deleteById(Long id);
    long countByServicioSinVariante(Long idServicio);
    long countByVariante(Long idVariante);
    void limpiarPrincipal(Long idServicio, Long idVariante);
}
