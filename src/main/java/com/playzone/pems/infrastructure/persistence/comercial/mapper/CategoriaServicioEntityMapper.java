package com.playzone.pems.infrastructure.persistence.comercial.mapper;

import com.playzone.pems.domain.comercial.model.CategoriaServicio;
import com.playzone.pems.infrastructure.persistence.comercial.entity.CategoriaServicioEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoriaServicioEntityMapper {

    public CategoriaServicio toDomain(CategoriaServicioEntity entity) {
        if (entity == null) return null;
        return CategoriaServicio.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .orden(entity.getOrden())
                .activo(entity.isActivo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public CategoriaServicioEntity toEntity(CategoriaServicio domain) {
        if (domain == null) return null;
        return CategoriaServicioEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .orden(domain.getOrden())
                .activo(domain.isActivo())
                .build();
    }
}
