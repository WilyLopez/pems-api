package com.playzone.pems.interfaces.rest.evento.request;

import com.playzone.pems.shared.validation.ContactoAdicionalValidator;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class SolicitarEventoPrivadoRequest {

    @NotNull
    private Long idTurno;

    @NotNull @Future
    private LocalDate fechaEvento;

    @NotBlank @Size(max = 120)
    private String tipoEvento;

    @Size(max = 200)
    @ContactoAdicionalValidator
    private String contactoAdicional;

    @Size(max = 30)
    private String origenContacto;

    @Min(1)
    private Integer aforoDeclarado;

    @Size(max = 120)
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Solo se permiten letras y espacios")
    private String nombreNino;

    @Min(0)
    private Integer edadCumple;

    private Long         idPaquete;
    private List<Long>   idsExtras;
    private List<@Size(max = 500) String> extrasLibres;

    @Size(max = 2000)
    private String observaciones;

    @Size(max = 4000)
    private String       descripcionPersonalizada;

    @DecimalMin(value = "0.0", message = "Debe ser mayor o igual a 0")
    private BigDecimal   presupuestoEstimado;
    private List<Long>   idsServiciosCotizacion;
    private Map<Long, Long> variantesSeleccionadas;
    private boolean      esCotizacionPersonalizada;
}
