package com.playzone.pems.infrastructure.persistence.cms.adapter;

import com.playzone.pems.domain.cms.model.ContenidoWeb;
import com.playzone.pems.domain.cms.repository.ContenidoWebRepository;
import com.playzone.pems.infrastructure.persistence.cms.jpa.ContenidoWebJpaRepository;
import com.playzone.pems.infrastructure.persistence.cms.mapper.CmsEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContenidoWebPersistenceAdapter implements ContenidoWebRepository {

    private final ContenidoWebJpaRepository contenidoJpa;
    private final CmsEntityMapper           mapper;

    @Override
    public Optional<ContenidoWeb> findById(Long id) {
        return contenidoJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ContenidoWeb> findBySeccionAndClave(String seccionCodigo, String clave) {
        return contenidoJpa.findBySeccionCodigoAndClave(seccionCodigo, clave).map(mapper::toDomain);
    }

    @Override
    public List<ContenidoWeb> findActivosBySeccion(String seccionCodigo) {
        return contenidoJpa.findBySeccionCodigoAndDeletedAtIsNull(seccionCodigo)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ContenidoWeb> findAllActivos() {
        return contenidoJpa.findByDeletedAtIsNull().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<ContenidoWeb> findVisibles() {
        return contenidoJpa.findByVisibleTrueAndDeletedAtIsNull()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<ContenidoWeb> findAll(String seccionCodigo, String clave, Pageable pageable) {
        String clavePattern = (clave != null && !clave.isBlank())
                ? "%" + clave.toLowerCase() + "%"
                : null;
        return contenidoJpa.findByFilters(seccionCodigo, clavePattern, pageable).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public ContenidoWeb save(ContenidoWeb c) {
        return mapper.toDomain(contenidoJpa.save(mapper.toEntity(c)));
    }

    @Override
    public boolean existsBySeccionAndClave(String seccionCodigo, String clave) {
        return contenidoJpa.existsBySeccionCodigoAndClave(seccionCodigo, clave);
    }
}
