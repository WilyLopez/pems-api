package com.playzone.pems.application.evento.dto.query;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class EventoServicioQuery {
    private Long       id;
    private Long       idServicioCotizacion;
    private Long       idServicioVariante;
    private String     nombre;
    private BigDecimal precioAcordado;
}
