package com.playzone.pems.application.finanzas.dto.command;

import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class AbrirCajaCommand {
    private Long           idSede;
    private TipoSesionCaja tipo;
    private BigDecimal     saldoInicial;
    private UUID           idUsuarioApertura;
    private String         observaciones;
}
