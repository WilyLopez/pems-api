package com.playzone.pems.domain.evento.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoExtra {

    private Long   id;
    private Long   idEventoPrivado;
    private Long   idExtra;
    private String nombreLibre;
    @Builder.Default
    private int    cantidad = 1;
    private String notas;
}
