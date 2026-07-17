package com.playzone.pems.domain.evento.model;

import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
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
public class ReservaPublica {

    private Long                 id;
    private Long                 ventaId;
    private Long                 idCliente;
    private Long                 idSede;
    private EstadoReservaPublica estado;
    private CanalReserva         canalReserva;
    private TipoDia              tipoDia;
    private Long                 idReservaOriginal;
    private boolean              esReprogramacion;
    private int                  vecesReprogramada;
    private LocalDate            fechaEvento;
    private String               numeroTicket;
    private BigDecimal           precioHistorico;
    private BigDecimal           descuentoAplicado;
    private BigDecimal           totalPagado;
    private String               nombreNino;
    private int                  edadNino;
    private String               nombreAcompanante;
    private String               dniAcompanante;
    private String               tipoDocumentoAcompanante;
    private boolean              firmoConsentimiento;
    private String               motivoCancelacion;
    private OffsetDateTime       createdAt;
    private OffsetDateTime       updatedAt;
    private boolean              ingresado;
    private OffsetDateTime       ingresoAt;
    private Integer              duracionHistoricaMinutos;
    private OffsetDateTime       permanenciaFinAt;
    private OffsetDateTime       salidaRealAt;
    private String               codigoQr;
    private UUID                 createdBy;
    private UUID                 updatedBy;
    private OffsetDateTime       deletedAt;

    public boolean puedeReprogramarse(int maxReprogramaciones) {
        return estado.esReprogramable() && vecesReprogramada < maxReprogramaciones;
    }

    public boolean puedeCancelarse() {
        return estado.esCancelable();
    }

    public boolean ocupaAforo() {
        return estado.ocupaAforo();
    }

    public boolean puedeRegistrarIngreso() {
        return !ingresado && estado == EstadoReservaPublica.CONFIRMADA;
    }

    public boolean requiresVentaForEntry() {
        return ventaId == null;
    }

    public boolean estaDentroDePermanencia(OffsetDateTime ahora) {
        return ingresado && permanenciaFinAt != null && !ahora.isAfter(permanenciaFinAt);
    }

    public boolean permanenciaVencida(OffsetDateTime ahora) {
        return ingresado && permanenciaFinAt != null && ahora.isAfter(permanenciaFinAt);
    }

    public boolean totalEsCoherente() {
        BigDecimal esperado = precioHistorico.subtract(descuentoAplicado);
        return totalPagado.compareTo(esperado) == 0;
    }
}