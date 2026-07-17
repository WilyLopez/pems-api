package com.playzone.pems.infrastructure.persistence.comercial.adapter;

import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioVarianteEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioCotizacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioVarianteJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.mapper.ServicioVarianteEntityMapper;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ServicioVariantePersistenceAdapter implements ServicioVarianteRepository {

    private final ServicioVarianteJpaRepository   jpaRepo;
    private final ServicioCotizacionJpaRepository servicioJpaRepo;
    private final ServicioVarianteEntityMapper    mapper;

    @Override
    public List<ServicioVariante> findByServicio(Long idServicio) {
        return jpaRepo.findByServicio_IdAndDeletedAtIsNullOrderByOrdenAsc(idServicio)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Map<Long, List<ServicioVariante>> findByServicios(List<Long> idsServicio) {
        if (idsServicio.isEmpty()) return Collections.emptyMap();
        return jpaRepo.findByServicio_IdInAndDeletedAtIsNullOrderByOrdenAsc(idsServicio)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.groupingBy(ServicioVariante::getIdServicio));
    }

    @Override
    public Optional<ServicioVariante> findById(Long id) {
        return jpaRepo.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public ServicioVariante save(ServicioVariante variante) {
        ServicioCotizacionEntity servicioEntity = servicioJpaRepo.findById(variante.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("ServicioCotizacion", variante.getIdServicio()));
        ServicioVarianteEntity entity = mapper.toEntity(variante, servicioEntity);
        return mapper.toDomain(jpaRepo.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepo.findById(id).ifPresent(entity -> {
            entity.setDeletedAt(OffsetDateTime.now());
            jpaRepo.save(entity);
        });
    }

    @Override
    public boolean existsByServicioAndNombre(Long idServicio, String nombre) {
        return jpaRepo.existsByServicio_IdAndNombreIgnoreCaseAndDeletedAtIsNull(idServicio, nombre);
    }

    @Override
    public boolean existsByServicioAndNombreExcludingId(Long idServicio, String nombre, Long id) {
        return jpaRepo.existsByServicio_IdAndNombreIgnoreCaseAndDeletedAtIsNullAndIdNot(idServicio, nombre, id);
    }
}
