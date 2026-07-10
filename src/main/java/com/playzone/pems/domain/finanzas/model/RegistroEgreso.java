package com.playzone.pems.domain.finanzas.model;

import com.playzone.pems.domain.finanzas.model.enums.EstadoAprobacionEgreso;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class RegistroEgreso {
    private Long          id;
    private String        tipoEgresoCodigo;
    private Long          idSede;
    private BigDecimal    monto;
    private LocalDate     fecha;
    private String        medioPago;
    private Integer       periodoAnio;
    private Integer       periodoMes;
    private String        descripcion;
    private String        comprobanteUrl;
    private boolean       esRecurrente;
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;
    private Long          idRegistroAnulado;
    @Builder.Default
    private EstadoAprobacionEgreso estadoAprobacion = EstadoAprobacionEgreso.APROBADO;
    private UUID          aprobadoPor;
    private OffsetDateTime fechaAprobacion;
    private String        motivoRechazo;
    private UUID          idUsuarioRegistra;
    private OffsetDateTime fechaCreacion;

    public boolean esContraasiento() {
        return naturaleza == NaturalezaMovimientoCaja.CONTRAASIENTO;
    }

    public boolean estaPendienteAprobacion() {
        return estadoAprobacion == EstadoAprobacionEgreso.PENDIENTE_APROBACION;
    }
}
