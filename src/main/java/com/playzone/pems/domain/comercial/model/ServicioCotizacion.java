package com.playzone.pems.domain.comercial.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ServicioCotizacion {
    private Long       id;
    private String     nombre;
    private String     descripcion;
    private BigDecimal precioReferencial;
    private String     icono;
    private boolean    activo;
    private boolean    destacado;
    private Long       categoriaId;
    private int        orden;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<ServicioVariante> variantes;
    private List<ServicioImagen>   imagenes;

    public boolean tieneVariantes() {
        return variantes != null && variantes.stream().anyMatch(ServicioVariante::isActivo);
    }

    public BigDecimal precioDesde() {
        if (variantes == null) return precioReferencial;
        return variantes.stream()
                .filter(ServicioVariante::isActivo)
                .map(ServicioVariante::getPrecio)
                .min(Comparator.naturalOrder())
                .orElse(precioReferencial);
    }

    public String imagenPrincipal() {
        if (imagenes == null || imagenes.isEmpty()) return null;
        return imagenes.stream()
                .filter(ServicioImagen::isEsPrincipal)
                .findFirst()
                .or(() -> imagenes.stream().min(Comparator.comparingInt(ServicioImagen::getOrden)))
                .map(ServicioImagen::getArchivoPath)
                .orElse(null);
    }
}
