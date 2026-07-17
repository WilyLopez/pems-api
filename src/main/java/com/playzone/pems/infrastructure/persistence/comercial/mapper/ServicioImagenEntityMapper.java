package com.playzone.pems.infrastructure.persistence.comercial.mapper;

import com.playzone.pems.domain.comercial.model.ServicioImagen;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioImagenEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioVarianteEntity;
import org.springframework.stereotype.Component;

@Component
public class ServicioImagenEntityMapper {

    public ServicioImagen toDomain(ServicioImagenEntity entity) {
        if (entity == null) return null;
        return ServicioImagen.builder()
                .id(entity.getId())
                .idServicio(entity.getServicio().getId())
                .idVariante(entity.getVariante() != null ? entity.getVariante().getId() : null)
                .archivoPath(entity.getArchivoPath())
                .altTexto(entity.getAltTexto())
                .orden(entity.getOrden())
                .esPrincipal(entity.isEsPrincipal())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public ServicioImagenEntity toEntity(ServicioImagen domain, ServicioCotizacionEntity servicio, ServicioVarianteEntity variante) {
        if (domain == null) return null;
        return ServicioImagenEntity.builder()
                .id(domain.getId())
                .servicio(servicio)
                .variante(variante)
                .archivoPath(domain.getArchivoPath())
                .altTexto(domain.getAltTexto())
                .orden(domain.getOrden())
                .esPrincipal(domain.isEsPrincipal())
                .build();
    }
}
