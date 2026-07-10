package com.playzone.pems.interfaces.rest.finanzas.response;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResumenCajaResponse {
    private Long                    id;
    private Long                    idSede;
    private UUID                    usuarioId;
    private TipoSesionCaja          tipo;
    private LocalDate               fecha;
    private BigDecimal              saldoInicial;
    private BigDecimal              totalIngresos;
    private BigDecimal              totalEgresos;
    private BigDecimal              saldoEsperado;
    private BigDecimal              saldoFinal;
    private BigDecimal              diferencia;
    private EstadoCaja              estado;
    private OffsetDateTime          fechaApertura;
    private OffsetDateTime          fechaCierre;
    private String                  observaciones;
    private List<MovimientoCajaResponse> movimientos;
    private List<ArqueoCajaResponse>     arqueos;
}
