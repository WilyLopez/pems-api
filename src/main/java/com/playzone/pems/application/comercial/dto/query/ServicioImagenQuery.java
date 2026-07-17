package com.playzone.pems.application.comercial.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioImagenQuery {
    private Long    id;
    private Long    idServicio;
    private Long    idVariante;
    private String  url;
    private String  altTexto;
    private int     orden;
    private boolean esPrincipal;
}
