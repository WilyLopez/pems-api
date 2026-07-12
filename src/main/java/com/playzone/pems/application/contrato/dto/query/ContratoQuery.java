package com.playzone.pems.application.contrato.dto.query;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder(toBuilder = true)
public class ContratoQuery {

    private Long           id;
    private Long           idEventoPrivado;
    private Long           idCliente;
    private String         archivoPdfUrl;
    private String         usuarioCarga;
    private OffsetDateTime fechaCarga;

    private String     nombreCliente;
    private String     correoCliente;
    private String     tipoEvento;
    private LocalDate  fechaEvento;
    private String     turno;
    private Integer    aforoDeclarado;
    private BigDecimal precioTotalContrato;
    private BigDecimal montoAdelanto;
    private BigDecimal saldoPendiente;

    private List<ActividadContratoQuery> actividades;
}
