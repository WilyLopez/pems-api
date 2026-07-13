package com.playzone.pems.application.venta.dto.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrarVentaMostradorCommand {
    private String                  tipoVenta;
    private Long                    sedeId;
    private Long                    clienteId;
    private LocalDate               fechaVisita;
    private List<NinoMostradorCommand>  ninos;
    private String                  nombreAcompanante;
    private String                  dniAcompanante;
    private String                  tipoDocumentoAcompanante;
    private String                  telefonoAcompanante;
    private Long                    idPromocion;
    private List<PagoMostradorCommand> pagos;
    private BigDecimal              efectivoRecibido;
    private boolean                 actaFirmada;
    private String                  notas;
}
