package com.playzone.pems.application.finanzas.dto.command;

import com.playzone.pems.domain.finanzas.model.enums.CategoriaRetiro;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
public class RegistrarMovimientoManualCommand {
    private Long               idSesionCaja;
    private TipoMovimientoCaja tipo;
    private String             concepto;
    private BigDecimal         monto;
    private String             medioPago;
    private CategoriaRetiro    categoriaRetiro;
    private UUID               idUsuarioRegistra;
}
