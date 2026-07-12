package com.playzone.pems.infrastructure.external.correo.renderizador;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ContenidoCorreo {

    private final String destinatario;
    private final String asunto;
    private final String cuerpoHtml;
    private final List<AdjuntoCorreo> adjuntos;
}
