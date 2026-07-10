package com.playzone.pems.domain.finanzas.model;

import com.playzone.pems.domain.finanzas.model.enums.CategoriaRetiro;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoCaja {
    private Long               id;
    private Long               idSesionCaja;
    private TipoMovimientoCaja tipo;
    private String             concepto;
    private BigDecimal         monto;
    private String             medioPago;
    private CategoriaRetiro    categoriaRetiro;
    private Long               idRegistroIngreso;
    private Long               idRegistroEgreso;
    private Long               idVenta;
    private boolean            esManual;
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;
    private Long               idMovimientoAnulado;
    private UUID               idUsuarioRegistra;
    private OffsetDateTime      fechaCreacion;

    public boolean esContraasiento() {
        return naturaleza == NaturalezaMovimientoCaja.CONTRAASIENTO;
    }
}
