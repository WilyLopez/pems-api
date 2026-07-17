package com.playzone.pems.domain.comercial.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServicioImagen {
    private Long    id;
    private Long    idServicio;
    private Long    idVariante;
    private String  archivoPath;
    private String  altTexto;
    private int     orden;
    private boolean esPrincipal;
    private OffsetDateTime createdAt;
}
