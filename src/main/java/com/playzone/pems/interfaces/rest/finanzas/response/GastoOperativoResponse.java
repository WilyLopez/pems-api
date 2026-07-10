package com.playzone.pems.interfaces.rest.finanzas.response;

import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class GastoOperativoResponse {
    private Long          id;
    private Long          idSede;
    private LocalDate     fecha;
    private String        descripcion;
    private BigDecimal    monto;
    private String        comprobanteUrl;
    private NaturalezaMovimientoCaja naturaleza;
    private Long          idGastoAnulado;
    private OffsetDateTime fechaCreacion;
}
