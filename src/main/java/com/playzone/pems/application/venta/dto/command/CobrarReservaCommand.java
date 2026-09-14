package com.playzone.pems.application.venta.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class CobrarReservaCommand {
    private final Long reservaId;
    private final List<PagoMostradorCommand> pagos;
    private final BigDecimal efectivoRecibido;
    private final boolean actaFirmada;
    private final String notas;
    private final java.util.UUID createdBy;
    private final String idempotencyKey;
}