package com.playzone.pems.domain.comercial.repository;

import com.playzone.pems.domain.comercial.model.ServicioVariante;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServicioVarianteRepository {
    List<ServicioVariante> findByServicio(Long idServicio);
    Map<Long, List<ServicioVariante>> findByServicios(List<Long> idsServicio);
    Optional<ServicioVariante> findById(Long id);
    ServicioVariante save(ServicioVariante variante);
    void deleteById(Long id);
    boolean existsByServicioAndNombre(Long idServicio, String nombre);
    boolean existsByServicioAndNombreExcludingId(Long idServicio, String nombre, Long id);
}
