package com.playzone.pems.interfaces.rest.evento.response;

import com.playzone.pems.application.evento.dto.query.EventoCuotaQuery;
import com.playzone.pems.application.evento.dto.query.EventoExtraQuery;
import com.playzone.pems.application.evento.dto.query.EventoServicioQuery;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
public class EventoPrivadoResponse {
    private final Long              id;
    private final Long              idCliente;
    private final String            nombreCliente;
    private final String            correoCliente;
    private final String            telefonoCliente;
    private final Long              idSede;
    private final String            estado;
    private final Long              idTurno;
    private final String            turno;
    private final String            horaInicio;
    private final String            horaFin;
    private final LocalDate         fechaEvento;
    private final String            tipoEvento;
    private final String            contactoAdicional;
    private final String            origenContacto;
    private final String            motivoCancelacion;
    private final Integer           aforoDeclarado;
    private final BigDecimal        precioTotalContrato;
    private final BigDecimal        montoAdelanto;
    private final BigDecimal        montoSaldo;
    private final String            observaciones;
    private final String            nombreNino;
    private final Integer           edadCumple;
    private final Long              idPaquete;
    private final String            descripcionPersonalizada;
    private final BigDecimal        presupuestoEstimado;
    private final boolean           esCotizacionPersonalizada;
    private final String            usuarioGestor;
    private final String            estadoOperativo;
    private final boolean           checklistCompleto;
    private final OffsetDateTime     horaInicioReal;
    private final OffsetDateTime     horaFinReal;
    private final List<EventoExtraQuery> extras;
    private final List<EventoServicioQuery> servicios;
    private final String            medioPago;
    private final OffsetDateTime     fechaCreacion;
    private final String            modalidadPago;
    private final LocalDate         fechaLimitePago;
    private final List<EventoCuotaQuery> cuotas;
}
