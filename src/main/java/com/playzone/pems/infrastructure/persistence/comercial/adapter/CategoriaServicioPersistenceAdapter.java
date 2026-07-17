package com.playzone.pems.infrastructure.persistence.comercial.adapter;

import com.playzone.pems.domain.comercial.model.CategoriaServicio;
import com.playzone.pems.domain.comercial.repository.CategoriaServicioRepository;
import com.playzone.pems.infrastructure.persistence.comercial.entity.CategoriaServicioEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.CategoriaServicioJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.mapper.CategoriaServicioEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CategoriaServicioPersistenceAdapter implements CategoriaServicioRepository {

    private final CategoriaServicioJpaRepository jpaRepo;
    private final CategoriaServicioEntityMapper  mapper;

    @Override
    public List<CategoriaServicio> findAllActivas() {
        return jpaRepo.findByActivoTrueOrderByOrdenAscNombreAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CategoriaServicio> findAll() {
        return jpaRepo.findAllByOrderByOrdenAscNombreAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<CategoriaServicio> findById(Long id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    public CategoriaServicio save(CategoriaServicio categoria) {
        CategoriaServicioEntity entity = mapper.toEntity(categoria);
        return mapper.toDomain(jpaRepo.save(entity));
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return jpaRepo.existsByNombreIgnoreCase(nombre);
    }

    @Override
    public boolean existsByNombreExcludingId(String nombre, Long id) {
        return jpaRepo.existsByNombreIgnoreCaseAndIdNot(nombre, id);
    }

    @Override
    public boolean tieneServiciosAsociados(Long id) {
        return jpaRepo.tieneServiciosActivos(id);
    }
}
