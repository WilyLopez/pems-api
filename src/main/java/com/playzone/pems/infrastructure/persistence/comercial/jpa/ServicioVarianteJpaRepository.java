package com.playzone.pems.infrastructure.persistence.comercial.jpa;

import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioVarianteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicioVarianteJpaRepository extends JpaRepository<ServicioVarianteEntity, Long> {
    List<ServicioVarianteEntity> findByServicio_IdAndDeletedAtIsNullOrderByOrdenAsc(Long idServicio);
    List<ServicioVarianteEntity> findByServicio_IdInAndDeletedAtIsNullOrderByOrdenAsc(List<Long> idsServicio);
    Optional<ServicioVarianteEntity> findByIdAndDeletedAtIsNull(Long id);
    boolean existsByServicio_IdAndNombreIgnoreCaseAndDeletedAtIsNull(Long idServicio, String nombre);
    boolean existsByServicio_IdAndNombreIgnoreCaseAndDeletedAtIsNullAndIdNot(Long idServicio, String nombre, Long id);
}
