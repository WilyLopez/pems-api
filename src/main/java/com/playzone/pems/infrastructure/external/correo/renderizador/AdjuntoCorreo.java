package com.playzone.pems.infrastructure.external.correo.renderizador;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdjuntoCorreo {

    private final String nombreArchivo;
    private final byte[] contenido;
    private final String tipoContenido;
}
