package com.playzone.pems.application.evento.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetalleQuery {
    private Long          idReserva;
    private String        numeroTicket;
    private String        estado;
    private boolean       yaIngreso;
    private OffsetDateTime fechaIngreso;
    private boolean       permanenciaVigente;
    private OffsetDateTime permanenciaFinAt;
    private LocalDate     fechaVisita;
    private boolean       esHoy;
    private String        nombreNino;
    private int           edadNino;
    private String        nombreAcompanante;
    private String        dniAcompanante;
    private BigDecimal    montoPagado;
    private String        estadoPago;
    private String        codigoQr;
}
