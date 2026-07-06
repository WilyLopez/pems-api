package com.playzone.pems.domain.usuario.model;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Sede {

    private Long          id;
    private String        nombre;
    private String        ciudad;
    private String        departamento;
    private String        ruc;

    private Double        latitud;
    private Double        longitud;

    private String        googleMapsEmbedUrl;

    private OffsetDateTime fechaCreacion;
    private OffsetDateTime deletedAt;

    public boolean tieneRucConfigurado() {
        return ruc != null && !ruc.isBlank();
    }

    public String etiqueta() {
        return nombre + " (" + ciudad + ")";
    }
}