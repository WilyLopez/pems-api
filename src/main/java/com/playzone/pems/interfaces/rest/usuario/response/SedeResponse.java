package com.playzone.pems.interfaces.rest.usuario.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class SedeResponse {
    private Long           id;
    private String         nombre;
    private String         ciudad;
    private String         departamento;
    private String         ruc;
    private Double         latitud;
    private Double         longitud;
    private String         googleMapsEmbedUrl;
    private boolean        activo;
    private OffsetDateTime fechaCreacion;
}
