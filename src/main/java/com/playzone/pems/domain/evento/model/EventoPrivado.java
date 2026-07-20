package com.playzone.pems.domain.evento.model;

import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.model.enums.ModalidadPago;
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
public class EventoPrivado {

    private Long                id;
    private Long                idCliente;
    private Long                idSede;
    private EstadoEventoPrivado estado;
    private Long                idTurno;
    private String              codigoTurno;
    private LocalDate           fechaEvento;
    private String              tipoEvento;
    private String              nombreTipoEvento;
    private String              contactoAdicional;
    private String              origenContacto;
    private Integer             aforoDeclarado;
    private BigDecimal          precioContrato;
    private BigDecimal          montoAdelanto;
    private String              motivoCancelacion;
    private String              notasInternas;
    private String              nombreNino;
    private Integer             edadCumple;
    private Long                paqueteId;
    private String              descripcionPersonalizada;
    private BigDecimal          presupuestoEstimado;
    private boolean             esCotizacionPersonalizada;
    private UUID                idUsuarioGestor;
    private OffsetDateTime      createdAt;
    private OffsetDateTime      updatedAt;
    private UUID                createdBy;
    private UUID                updatedBy;
    private String              estadoOperativo;
    private boolean             checklistCompleto;
    private OffsetDateTime      horaInicioReal;
    private OffsetDateTime      horaFinReal;
    @Builder.Default
    private ModalidadPago       modalidadPago = ModalidadPago.AL_CONTADO;
    private LocalDate           fechaLimitePago;

    public BigDecimal calcularMontoSaldo() {
        if (precioContrato == null) return null;
        BigDecimal adelanto = montoAdelanto != null ? montoAdelanto : BigDecimal.ZERO;
        return precioContrato.subtract(adelanto);
    }

    public boolean puedeCancelarse() {
        return estado.esCancelable();
    }

    public boolean bloqueaAccesoPublico() {
        return estado.bloqueaDisponibilidadPublica();
    }

    public boolean adelantoCubreMitad() {
        if (precioContrato == null || montoAdelanto == null) return false;
        return montoAdelanto.multiply(BigDecimal.valueOf(2))
                .compareTo(precioContrato) >= 0;
    }

    public boolean estaSaldado() {
        BigDecimal saldo = calcularMontoSaldo();
        return saldo != null && saldo.compareTo(BigDecimal.ZERO) == 0;
    }
}