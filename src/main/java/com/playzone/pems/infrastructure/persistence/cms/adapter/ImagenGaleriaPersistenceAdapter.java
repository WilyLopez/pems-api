package com.playzone.pems.infrastructure.persistence.cms.adapter;

import com.playzone.pems.domain.cms.model.ImagenGaleria;
import com.playzone.pems.domain.cms.model.enums.CategoriaImagen;
import com.playzone.pems.domain.cms.repository.ImagenGaleriaRepository;
import com.playzone.pems.infrastructure.persistence.cms.entity.ImagenGaleriaEntity;
import com.playzone.pems.infrastructure.persistence.cms.jpa.ImagenGaleriaJpaRepository;
import com.playzone.pems.infrastructure.persistence.cms.mapper.CmsEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImagenGaleriaPersistenceAdapter implements ImagenGaleriaRepository {

    private final ImagenGaleriaJpaRepository jpaRepository;
    private final SedeJpaRepository          sedeRepository;
    private final CmsEntityMapper            mapper;

    @Override
    public Optional<ImagenGaleria> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ImagenGaleria> findActivasBySede(Long idSede) {
        return jpaRepository.findBySede_IdAndActivoTrueOrderByOrdenVisualizacionAsc(idSede)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ImagenGaleria> findActivasBySedeAndCategoria(Long idSede, CategoriaImagen categoria) {
        return jpaRepository.findBySede_IdAndCategoriaImagenAndActivoTrueOrderByOrdenVisualizacionAsc(idSede, categoria)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<ImagenGaleria> findBySede(Long idSede, Pageable pageable) {
        Page<ImagenGaleriaEntity> pagina = idSede == null
                ? jpaRepository.findAll(pageable)
                : jpaRepository.findBySede_Id(idSede, pageable);
        return pagina.map(mapper::toDomain);
    }

    @Override
    public Page<ImagenGaleria> findBySedeAndDestacada(Long idSede, boolean destacada, Pageable pageable) {
        Page<ImagenGaleriaEntity> pagina = idSede == null
                ? jpaRepository.findByDestacada(destacada, pageable)
                : jpaRepository.findBySede_IdAndDestacada(idSede, destacada, pageable);
        return pagina.map(mapper::toDomain);
    }

    @Override
    public ImagenGaleria save(ImagenGaleria imagen) {
        var sedeEntity = imagen.getIdSede() != null
                ? sedeRepository.findById(imagen.getIdSede()).orElse(null)
                : sedeRepository.findFirstByDeletedAtIsNullOrderByIdAsc().orElse(null);
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(imagen, sedeEntity)));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }
}
