package com.playzone.pems.application.finanzas.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class RegistrarEgresoCommand {
    private String     tipoEgresoCodigo;
    private Long       idSede;
    private BigDecimal monto;
    private LocalDate  fecha;
    private String     medioPago;
    private Integer    periodoAnio;
    private Integer    periodoMes;
    private String     descripcion;
    private String     comprobanteUrl;
    private boolean    esRecurrente;
    private UUID       idUsuarioRegistra;
}
