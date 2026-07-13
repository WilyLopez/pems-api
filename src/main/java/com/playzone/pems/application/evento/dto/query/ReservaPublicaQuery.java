package com.playzone.pems.application.evento.dto.query;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
public class ReservaPublicaQuery {

    private final Long          id;
    private final Long          idCliente;
    private final String        nombreCliente;
    private final String        correoCliente;
    private final Long          idSede;
    private final String        nombreSede;
    private final String        estado;
    private final String        canalReserva;
    private final String        tipoDia;
    private final LocalDate     fechaEvento;
    private final String        numeroTicket;
    private final BigDecimal    precioHistorico;
    private final BigDecimal    descuentoAplicado;
    private final BigDecimal    totalPagado;
    private final String        nombreNino;
    private final int           edadNino;
    private final String        nombreAcompanante;
    private final String        dniAcompanante;
    private final boolean       firmoConsentimiento;
    private final boolean       esReprogramacion;
    private final int           vecesReprogramada;
    private final boolean       ingresado;
    private final OffsetDateTime fechaIngreso;
    private final String        codigoQr;
    private final String        medioPago;
    private final String        referenciaPago;
    private final String        motivoRechazoPago;
    private final String        motivoCancelacion;
    private final OffsetDateTime fechaCreacion;
}