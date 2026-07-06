package com.playzone.pems.infrastructure.persistence.cms.jpa;

import com.playzone.pems.domain.cms.model.enums.CategoriaImagen;
import com.playzone.pems.infrastructure.persistence.cms.entity.ImagenGaleriaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImagenGaleriaJpaRepository extends JpaRepository<ImagenGaleriaEntity, Long> {

    List<ImagenGaleriaEntity> findBySede_IdAndActivoTrueOrderByOrdenVisualizacionAsc(Long idSede);

    List<ImagenGaleriaEntity> findBySede_IdAndCategoriaImagenAndActivoTrueOrderByOrdenVisualizacionAsc(
            Long idSede, CategoriaImagen categoria);

    Page<ImagenGaleriaEntity> findBySede_Id(Long idSede, Pageable pageable);

    Page<ImagenGaleriaEntity> findBySede_IdAndDestacada(Long idSede, boolean destacada, Pageable pageable);

    Page<ImagenGaleriaEntity> findByDestacada(boolean destacada, Pageable pageable);
}
