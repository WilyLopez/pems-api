package com.playzone.pems.infrastructure.persistence.comercial.mapper;

import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioVarianteEntity;
import org.springframework.stereotype.Component;

@Component
public class ServicioVarianteEntityMapper {

    public ServicioVariante toDomain(ServicioVarianteEntity entity) {
        if (entity == null) return null;
        return ServicioVariante.builder()
                .id(entity.getId())
                .idServicio(entity.getServicio().getId())
                .nombre(entity.getNombre())
                .descripcion(entity.getDescripcion())
                .precio(entity.getPrecio())
                .activo(entity.isActivo())
                .orden(entity.getOrden())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public ServicioVarianteEntity toEntity(ServicioVariante domain, ServicioCotizacionEntity servicio) {
        if (domain == null) return null;
        return ServicioVarianteEntity.builder()
                .id(domain.getId())
                .servicio(servicio)
                .nombre(domain.getNombre())
                .descripcion(domain.getDescripcion())
                .precio(domain.getPrecio())
                .activo(domain.isActivo())
                .orden(domain.getOrden())
                .build();
    }
}
