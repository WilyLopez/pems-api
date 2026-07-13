package com.playzone.pems.interfaces.rest.venta.request;

import com.playzone.pems.application.venta.dto.command.NinoMostradorCommand;
import com.playzone.pems.application.venta.dto.command.PagoMostradorCommand;
import com.playzone.pems.application.venta.dto.command.RegistrarVentaMostradorCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class RegistrarVentaMostradorRequest {

    @NotBlank
    @Pattern(regexp = "RESERVA",
            message = "Tipo de venta no soportado en esta fase. Solo se acepta RESERVA.")
    private String tipoVenta;

    @NotNull
    private Long sedeId;

    private Long clienteId;

    @NotNull(message = "La fecha de visita es obligatoria.")
    private LocalDate fechaVisita;

    @NotEmpty(message = "Debe registrar al menos un nino.")
    @Valid
    private List<NinoRequest> ninos;

    @Size(max = 150)
    private String nombreAcompanante;

    @Size(max = 15)
    private String dniAcompanante;

    @Pattern(regexp = "DNI|RUC", message = "Tipo de documento del acompanante no valido.")
    private String tipoDocumentoAcompanante;

    @Size(max = 15)
    private String telefonoAcompanante;

    private Long idPromocion;

    @NotEmpty(message = "Debe registrar al menos un metodo de pago.")
    @Valid
    private List<PagoRequest> pagos;

    private BigDecimal efectivoRecibido;

    @AssertTrue(message = "Debe confirmar la firma del Acta de Responsabilidad.")
    private boolean actaFirmada;

    private String notas;

    public RegistrarVentaMostradorCommand toCommand() {
        List<NinoMostradorCommand> ninosCmd = ninos.stream()
                .map(n -> NinoMostradorCommand.builder()
                        .nombreNino(n.getNombreNino())
                        .edadNino(n.getEdadNino())
                        .build())
                .toList();

        List<PagoMostradorCommand> pagosCmd = pagos.stream()
                .map(p -> PagoMostradorCommand.builder()
                        .medioPago(p.getMedioPago())
                        .monto(p.getMonto())
                        .referencia(p.getReferencia())
                        .build())
                .toList();

        return RegistrarVentaMostradorCommand.builder()
                .tipoVenta(tipoVenta)
                .sedeId(sedeId)
                .clienteId(clienteId)
                .fechaVisita(fechaVisita)
                .ninos(ninosCmd)
                .nombreAcompanante(nombreAcompanante)
                .dniAcompanante(dniAcompanante)
                .tipoDocumentoAcompanante(tipoDocumentoAcompanante)
                .telefonoAcompanante(telefonoAcompanante)
                .idPromocion(idPromocion)
                .pagos(pagosCmd)
                .efectivoRecibido(efectivoRecibido)
                .actaFirmada(actaFirmada)
                .notas(notas)
                .build();
    }

    @Getter
    @NoArgsConstructor
    public static class NinoRequest {
        @NotBlank(message = "El nombre del nino es obligatorio.")
        @Size(min = 2, max = 120)
        private String nombreNino;

        @Min(0) @Max(17)
        private int edadNino;
    }

    @Getter
    @NoArgsConstructor
    public static class PagoRequest {
        @NotBlank
        @Pattern(regexp = "EFECTIVO|YAPE|TARJETA|PLIN|TRANSFERENCIA",
                message = "Medio de pago no valido.")
        private String medioPago;

        @NotNull
        @DecimalMin(value = "0.01", message = "El monto debe ser positivo.")
        private BigDecimal monto;

        private String referencia;
    }
}
