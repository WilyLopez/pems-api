package com.playzone.pems.domain.finanzas.model;

import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class SesionCaja {
    private Long           id;
    private Long           idSede;
    private UUID           usuarioId;
    private TipoSesionCaja tipo;
    private EstadoCaja     estado;
    private BigDecimal     saldoInicial;
    private BigDecimal     totalIngresos;
    private BigDecimal     totalEgresos;
    private BigDecimal     saldoEsperado;
    private BigDecimal     saldoFinal;
    private BigDecimal     diferencia;
    private OffsetDateTime fechaApertura;
    private OffsetDateTime fechaCierre;
    private UUID           cerradaPor;
    private String         motivoCierre;
    private String         observaciones;
    private OffsetDateTime fechaCreacion;

    public boolean estaAbierta() {
        return estado == EstadoCaja.ABIERTA;
    }

    public BigDecimal calcularSaldoEsperado() {
        BigDecimal inicial  = saldoInicial  != null ? saldoInicial  : BigDecimal.ZERO;
        BigDecimal ingresos = totalIngresos != null ? totalIngresos : BigDecimal.ZERO;
        BigDecimal egresos  = totalEgresos  != null ? totalEgresos  : BigDecimal.ZERO;
        return inicial.add(ingresos).subtract(egresos);
    }
}
