package com.playzone.pems.application.comercial.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubirServicioImagenCommand {
    private byte[] contenido;
    private String nombreArchivo;
    private String contentType;
    private String altTexto;
    private Long   idVariante;
    private int    orden;
}
