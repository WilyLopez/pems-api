package com.playzone.pems.domain.evento.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoServicio {

    private Long       id;
    private Long       idEventoPrivado;
    private Long       idServicioCotizacion;
    private Long       idServicioVariante;
    private String     nombreLibre;
    private String     descripcion;
    private BigDecimal precioAcordado;
    private boolean    incluido;
}
