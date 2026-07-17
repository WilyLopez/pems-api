package com.playzone.pems.interfaces.rest.comercial.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ServicioImagenResponse {
    private final Long    id;
    private final Long    idServicio;
    private final Long    idVariante;
    private final String  url;
    private final String  altTexto;
    private final int     orden;
    private final boolean esPrincipal;
}
