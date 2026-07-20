package com.playzone.pems.application.evento.dto.command;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class SolicitarEventoPrivadoCommand {

    @NotNull
    private final Long idCliente;

    @NotNull
    private final Long idSede;

    @NotNull
    private final Long idTurno;

    @NotNull
    @Future
    private final LocalDate fechaEvento;

    @NotBlank
    @Size(max = 200)
    private final String tipoEvento;

    @Size(max = 200)
    private final String contactoAdicional;

    @Size(max = 30)
    private final String origenContacto;

    @Min(1)
    private final Integer aforoDeclarado;

    @Size(max = 120)
    private final String nombreNino;

    @Min(0)
    private final Integer edadCumple;

    private final Long         idPaquete;
    private final List<Long>   idsExtras;
    private final List<String> extrasLibres;

    @Size(max = 2000)
    private final String       observaciones;

    @Size(max = 4000)
    private final String       descripcionPersonalizada;
    private final BigDecimal   presupuestoEstimado;
    private final List<Long>   idsServiciosCotizacion;
    private final Map<Long, Long> variantesSeleccionadas;
    private final boolean      esCotizacionPersonalizada;
}
