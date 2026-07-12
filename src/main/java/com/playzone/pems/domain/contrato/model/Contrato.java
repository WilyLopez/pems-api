package com.playzone.pems.domain.contrato.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Contrato {

    private Long           id;
    private Long           idEventoPrivado;
    private Long           idCliente;
    private String         archivoPdfUrl;
    private UUID           idUsuarioCarga;
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
}
