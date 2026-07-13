package com.playzone.pems.domain.venta.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VentaPago {
    private Long          id;
    private Long          ventaId;
    private String        medioPagoCodigo;
    private BigDecimal    monto;
    private String        referencia;
    private String        motivoRechazo;
    private boolean       esValidado;
    private UUID          validadoPor;
    private OffsetDateTime validadoAt;
    private OffsetDateTime createdAt;
}
