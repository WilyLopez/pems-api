package com.playzone.pems.domain.cms.repository;

import com.playzone.pems.domain.cms.model.ContenidoWeb;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ContenidoWebRepository {

    Optional<ContenidoWeb> findById(Long id);

    Optional<ContenidoWeb> findBySeccionAndClave(String seccionCodigo, String clave);

    List<ContenidoWeb> findActivosBySeccion(String seccionCodigo);

    List<ContenidoWeb> findAllActivos();

    List<ContenidoWeb> findVisibles();

    Page<ContenidoWeb> findAll(String seccionCodigo, String clave, Pageable pageable);

    ContenidoWeb save(ContenidoWeb contenido);

    boolean existsBySeccionAndClave(String seccionCodigo, String clave);
}