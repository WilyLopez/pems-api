package com.playzone.pems.application.comercial.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCotizacionQuery {
    private Long          id;
    private String        nombre;
    private String        descripcion;
    private BigDecimal    precioReferencial;
    private String        icono;
    private boolean       activo;
    private boolean       destacado;
    private Long          categoriaId;
    private String        categoriaNombre;
    private int           orden;
    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
    private boolean       tieneVariantes;
    private BigDecimal    precioDesde;
    private List<ServicioVarianteQuery> variantes;
    private String        imagenPrincipal;
    private List<ServicioImagenQuery> imagenes;
}
