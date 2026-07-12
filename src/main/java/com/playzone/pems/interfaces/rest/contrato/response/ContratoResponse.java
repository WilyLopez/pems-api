package com.playzone.pems.interfaces.rest.contrato.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContratoResponse {

    private Long            id;
    private Long            idEventoPrivado;
    private String          usuarioCarga;
    private OffsetDateTime  fechaCarga;

    private String     nombreCliente;
    private String     correoCliente;
    private String     tipoEvento;
    private LocalDate  fechaEvento;
    private String     turno;
    private Integer    aforoDeclarado;
    private BigDecimal precioTotalContrato;
    private BigDecimal montoAdelanto;
    private BigDecimal saldoPendiente;

    private List<ActividadContratoResponse> actividades;

    @Getter
    @Builder
    public static class ActividadContratoResponse {
        private Long           id;
        private String         accion;
        private String         descripcion;
        private String         usuario;
        private OffsetDateTime fechaAccion;
    }
}
