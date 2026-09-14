package com.playzone.pems.interfaces.rest.venta.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@NoArgsConstructor
public class CobrarReservaRequest {

    @NotEmpty(message = "Debe registrar al menos un metodo de pago.")
    @Valid
    private List<RegistrarVentaMostradorRequest.PagoRequest> pagos;

    @DecimalMin(value = "0.00", message = "El efectivo recibido no puede ser negativo.")
    private BigDecimal efectivoRecibido;

    @AssertTrue(message = "Debe confirmar la firma del Acta de Responsabilidad.")
    private boolean actaFirmada;

    private String notas;

    @Size(max = 100)
    private String idempotencyKey;
}