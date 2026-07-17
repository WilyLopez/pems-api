package com.playzone.pems.infrastructure.persistence.comercial.adapter;

import com.playzone.pems.domain.comercial.model.ServicioImagen;
import com.playzone.pems.domain.comercial.repository.ServicioImagenRepository;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioImagenEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioVarianteEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioCotizacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioImagenJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioVarianteJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.mapper.ServicioImagenEntityMapper;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServicioImagenPersistenceAdapter implements ServicioImagenRepository {

    private final ServicioImagenJpaRepository     jpaRepo;
    private final ServicioCotizacionJpaRepository servicioJpaRepo;
    private final ServicioVarianteJpaRepository   varianteJpaRepo;
    private final ServicioImagenEntityMapper      mapper;

    @Override
    public List<ServicioImagen> findByServicio(Long idServicio) {
        return jpaRepo.findByServicio_IdOrderByOrdenAsc(idServicio)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Map<Long, List<ServicioImagen>> findByServicios(List<Long> idsServicio) {
        if (idsServicio.isEmpty()) return Collections.emptyMap();
        return jpaRepo.findByServicio_IdInOrderByOrdenAsc(idsServicio)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.groupingBy(ServicioImagen::getIdServicio));
    }

    @Override
    public Optional<ServicioImagen> findById(Long id) {
        return jpaRepo.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public ServicioImagen save(ServicioImagen imagen) {
        ServicioCotizacionEntity servicioEntity = servicioJpaRepo.findById(imagen.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("ServicioCotizacion", imagen.getIdServicio()));
        ServicioVarianteEntity varianteEntity = imagen.getIdVariante() != null
                ? varianteJpaRepo.findById(imagen.getIdVariante())
                        .orElseThrow(() -> new ResourceNotFoundException("ServicioVariante", imagen.getIdVariante()))
                : null;
        ServicioImagenEntity entity = mapper.toEntity(imagen, servicioEntity, varianteEntity);
        return mapper.toDomain(jpaRepo.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public long countByServicioSinVariante(Long idServicio) {
        return jpaRepo.countByServicio_IdAndVarianteIsNull(idServicio);
    }

    @Override
    public long countByVariante(Long idVariante) {
        return jpaRepo.countByVariante_Id(idVariante);
    }

    @Override
    @Transactional
    public void limpiarPrincipal(Long idServicio, Long idVariante) {
        jpaRepo.limpiarPrincipal(idServicio, idVariante);
    }
}
