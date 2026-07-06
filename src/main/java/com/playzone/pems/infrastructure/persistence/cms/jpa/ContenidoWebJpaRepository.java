package com.playzone.pems.infrastructure.persistence.cms.jpa;

import com.playzone.pems.infrastructure.persistence.cms.entity.ContenidoWebEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContenidoWebJpaRepository extends JpaRepository<ContenidoWebEntity, Long> {

    Optional<ContenidoWebEntity> findBySeccionCodigoAndClave(String seccionCodigo, String clave);

    List<ContenidoWebEntity> findBySeccionCodigoAndDeletedAtIsNull(String seccionCodigo);

    List<ContenidoWebEntity> findByDeletedAtIsNull();

    List<ContenidoWebEntity> findByVisibleTrueAndDeletedAtIsNull();

    @Query("SELECT c FROM ContenidoWebEntity c " +
           "WHERE (:seccionCodigo IS NULL OR c.seccionCodigo = :seccionCodigo) " +
           "AND (:clavePattern IS NULL OR LOWER(c.clave) LIKE :clavePattern)")
    Page<ContenidoWebEntity> findByFilters(
            @Param("seccionCodigo") String seccionCodigo,
            @Param("clavePattern") String clavePattern,
            Pageable pageable);

    boolean existsBySeccionCodigoAndClave(String seccionCodigo, String clave);
}
