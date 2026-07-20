package com.playzone.pems.application.evento.dto.query;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class EventoExtraQuery {
    private Long   id;
    private Long   idExtra;
    private String nombreExtra;
    private String nombreLibre;
    private int    cantidad;
    private String notas;
}
