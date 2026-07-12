package com.playzone.pems.application.contrato.dto.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ArchivoContratoQuery {
    private byte[] contenido;
    private String nombreArchivo;
}
