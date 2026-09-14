package com.playzone.pems.application.finanzas.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class AbrirCajaCommand {
    private Long       idSede;
    private BigDecimal saldoInicial;
    private UUID       idUsuarioApertura;
    private String     observaciones;
}
