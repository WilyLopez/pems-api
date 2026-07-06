package com.playzone.pems.infrastructure.persistence.usuario.mapper;

import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SedeEntityMapper {

    public Sede toDomain(SedeEntity e) {
        if (e == null) return null;
        return Sede.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .ciudad(e.getCiudad())
                .departamento(e.getDepartamento())
                .ruc(e.getRuc())
                .latitud(e.getLatitud())
                .longitud(e.getLongitud())
                .googleMapsEmbedUrl(e.getGoogleMapsEmbedUrl())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .deletedAt(e.getDeletedAt() != null ? e.getDeletedAt() : null)
                .build();
    }

    public SedeEntity toEntity(Sede d) {
        if (d == null) return null;
        return SedeEntity.builder()
                .id(d.getId())
                .nombre(d.getNombre())
                .ciudad(d.getCiudad())
                .departamento(d.getDepartamento())
                .ruc(d.getRuc())
                .latitud(d.getLatitud())
                .longitud(d.getLongitud())
                .googleMapsEmbedUrl(d.getGoogleMapsEmbedUrl())
                .build();
    }

}
