package com.playzone.pems.interfaces.rest.comercial.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class ServicioCotizacionResponse {
    private final Long       id;
    private final String     nombre;
    private final String     descripcion;
    private final BigDecimal precioReferencial;
    private final String     icono;
    private final boolean    activo;
    private final boolean    destacado;
    private final Long       categoriaId;
    private final String     categoriaNombre;
    private final int        orden;
    private final OffsetDateTime fechaCreacion;
    private final OffsetDateTime fechaActualizacion;
    private final boolean    tieneVariantes;
    private final BigDecimal precioDesde;
    private final List<ServicioVarianteResponse> variantes;
    private final String     imagenPrincipal;
    private final List<ServicioImagenResponse> imagenes;
}
