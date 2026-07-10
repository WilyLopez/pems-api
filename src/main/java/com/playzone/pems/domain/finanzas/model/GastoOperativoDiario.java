package com.playzone.pems.domain.finanzas.model;

import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GastoOperativoDiario {
    private Long          id;
    private Long          idSede;
    private LocalDate     fecha;
    private String        descripcion;
    private BigDecimal    monto;
    private String        comprobanteUrl;
    private UUID          idUsuarioRegistra;
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;
    private Long          idGastoAnulado;
    private OffsetDateTime fechaCreacion;

    public boolean esContraasiento() {
        return naturaleza == NaturalezaMovimientoCaja.CONTRAASIENTO;
    }
}
