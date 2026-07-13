package com.playzone.pems.domain.venta.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    private Long          id;
    private Long          idSede;
    private Long          clienteId;
    private Long          eventoId;
    private String        tipo;
    private String        canalCodigo;
    private LocalDate     fechaVisita;
    private String        nombreAcompanante;
    private String        dniAcompanante;
    private String        tipoDocumentoAcompanante;
    private String        telefonoAcompanante;
    private Long          promocionId;
    private BigDecimal    subtotal;
    private BigDecimal    descuento;
    private BigDecimal    total;
    private BigDecimal    efectivoRecibido;
    private BigDecimal    vuelto;
    private boolean       actaFirmada;
    private boolean       esAnticipada;
    private boolean       impreso;
    private boolean       enviadoCorreo;
    private boolean       descargado;
    private String        notas;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID          createdBy;
    private OffsetDateTime deletedAt;

    public boolean totalEsCoherente() {
        BigDecimal esperado = subtotal.subtract(descuento);
        return total.compareTo(esperado) == 0;
    }

    public boolean estaVinculadaAEvento() {
        return eventoId != null;
    }

    public boolean tuvoDescuento() {
        return descuento != null && descuento.compareTo(BigDecimal.ZERO) > 0;
    }
}
