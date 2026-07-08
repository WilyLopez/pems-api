package com.playzone.pems.application.finanzas.dto.query;

import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ResumenCajaQuery {
    private Long               id;
    private Long               idSede;
    private UUID               usuarioId;
    private TipoSesionCaja     tipo;
    private LocalDate          fecha;
    private BigDecimal         saldoInicial;
    private BigDecimal         totalIngresos;
    private BigDecimal         totalEgresos;
    private BigDecimal         saldoEsperado;
    private BigDecimal         saldoFinal;
    private BigDecimal         diferencia;
    private EstadoCaja         estado;
    private OffsetDateTime     fechaApertura;
    private OffsetDateTime     fechaCierre;
    private String             observaciones;
    private List<MovimientoCajaQuery> movimientos;
    private List<ArqueoCajaQuery>     arqueos;
}
