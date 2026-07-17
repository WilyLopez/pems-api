package com.playzone.pems.infrastructure.persistence.comercial.mapper;

import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import org.springframework.stereotype.Component;

@Component
public class ServicioCotizacionEntityMapper {

    public ServicioCotizacion toDomain(ServicioCotizacionEntity entity) {
        if (entity == null) return null;
        return ServicioCotizacion.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .precioReferencial(entity.getPrecioReferencial())
                .icono(entity.getIcono())
                .activo(entity.isActivo())
                .destacado(entity.isDestacado())
                .categoriaId(entity.getCategoriaId())
                .orden(entity.getOrden())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ServicioCotizacionEntity toEntity(ServicioCotizacion domain) {
        if (domain == null) return null;
        return ServicioCotizacionEntity.builder()
                .id(domain.getId())
                .nombre(domain.getNombre())
                .descripcion(domain.getDescripcion())
                .precioReferencial(domain.getPrecioReferencial())
                .icono(domain.getIcono())
                .activo(domain.isActivo())
                .destacado(domain.isDestacado())
                .categoriaId(domain.getCategoriaId())
                .orden(domain.getOrden())
                .build();
    }
}
