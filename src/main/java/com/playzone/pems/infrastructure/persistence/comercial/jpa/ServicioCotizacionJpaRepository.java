package com.playzone.pems.infrastructure.persistence.comercial.jpa;

import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioCotizacionJpaRepository extends JpaRepository<ServicioCotizacionEntity, Long> {
    List<ServicioCotizacionEntity> findByActivoTrueAndDeletedAtIsNullOrderByOrdenAsc();
    List<ServicioCotizacionEntity> findByDeletedAtIsNullOrderByOrdenAsc();
    Optional<ServicioCotizacionEntity> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByNombreIgnoreCaseAndDeletedAtIsNull(String nombre);
    boolean existsByNombreIgnoreCaseAndDeletedAtIsNullAndIdNot(String nombre, Long id);
}
