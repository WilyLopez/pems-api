package com.playzone.pems.domain.evento.model;

import com.playzone.pems.domain.evento.model.enums.EstadoCuota;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class EventoCuota {

    private Long          id;
    private Long          eventoId;
    private int           numeroCuota;
    private BigDecimal    monto;
    private LocalDate     fechaVencimiento;
    private EstadoCuota   estado;
    private Long          ventaId;
    private OffsetDateTime createdAt;

    public boolean esPagado()    { return estado == EstadoCuota.PAGADO; }
    public boolean esPendiente() { return estado == EstadoCuota.PENDIENTE; }
    public boolean esVencido()   { return estado == EstadoCuota.VENCIDO; }
}
