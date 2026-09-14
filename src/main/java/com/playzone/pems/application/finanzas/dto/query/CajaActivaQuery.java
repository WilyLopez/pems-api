package com.playzone.pems.application.finanzas.dto.query;

import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class CajaActivaQuery {
    private Long                       id;
    private Long                       idSede;
    private UUID                       usuarioId;
    private String                     nombreCajero;
    private EstadoCaja                 estado;
    private LocalDate                  fecha;
    private BigDecimal                 saldoInicial;
    private BigDecimal                 totalIngresos;
    private BigDecimal                 totalEgresos;
    private BigDecimal                 saldoEsperado;
    private OffsetDateTime             fechaApertura;
    private String                     observaciones;
    private int                        cantidadVentas;
    private BigDecimal                 totalVendido;
    private Map<String, BigDecimal>    desglosePorMedioPago;
    private List<MovimientoCajaQuery>  movimientos;
    private List<ArqueoCajaQuery>      arqueos;
}
