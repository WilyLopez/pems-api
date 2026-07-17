package com.playzone.pems.infrastructure.persistence.comercial.jpa;

import com.playzone.pems.infrastructure.persistence.comercial.entity.CategoriaServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CategoriaServicioJpaRepository extends JpaRepository<CategoriaServicioEntity, Long> {

    List<CategoriaServicioEntity> findAllByOrderByOrdenAscNombreAsc();

    List<CategoriaServicioEntity> findByActivoTrueOrderByOrdenAscNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Query("SELECT COUNT(s) > 0 FROM ServicioCotizacionEntity s WHERE s.categoriaId = :id AND s.deletedAt IS NULL")
    boolean tieneServiciosActivos(@Param("id") Long id);
}
