package com.playzone.pems.infrastructure.persistence.comercial.adapter;

import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.jpa.ServicioCotizacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.comercial.mapper.ServicioCotizacionEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ServicioCotizacionPersistenceAdapter implements ServicioCotizacionRepository {

    private final ServicioCotizacionJpaRepository jpaRepo;
    private final ServicioCotizacionEntityMapper  mapper;

    @Override
    public List<ServicioCotizacion> findAllActivos() {
        return jpaRepo.findByActivoTrueAndDeletedAtIsNullOrderByOrdenAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ServicioCotizacion> findAll() {
        return jpaRepo.findByDeletedAtIsNullOrderByOrdenAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ServicioCotizacion> findById(Long id) {
        return jpaRepo.findByIdAndDeletedAtIsNull(id).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public ServicioCotizacion save(ServicioCotizacion s) {
        ServicioCotizacionEntity entity = mapper.toEntity(s);
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
    public boolean existsByNombre(String nombre) {
        return jpaRepo.existsByNombreIgnoreCaseAndDeletedAtIsNull(nombre);
    }

    @Override
    public boolean existsByNombreExcludingId(String nombre, Long id) {
        return jpaRepo.existsByNombreIgnoreCaseAndDeletedAtIsNullAndIdNot(nombre, id);
    }
}
