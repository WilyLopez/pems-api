package com.playzone.pems.interfaces.rest.finanzas.response;

import com.playzone.pems.domain.finanzas.model.enums.EstadoAprobacionEgreso;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
public class RegistroEgresoResponse {
    private Long          id;
    private String        tipoEgresoCodigo;
    private Long          idSede;
    private BigDecimal    monto;
    private LocalDate     fecha;
    private String        medioPago;
    private Integer       periodoAnio;
    private Integer       periodoMes;
    private String        descripcion;
    private String        comprobanteUrl;
    private boolean       esRecurrente;
    private NaturalezaMovimientoCaja naturaleza;
    private Long          idRegistroAnulado;
    private EstadoAprobacionEgreso estadoAprobacion;
    private OffsetDateTime fechaAprobacion;
    private String        motivoRechazo;
    private OffsetDateTime fechaCreacion;
}
