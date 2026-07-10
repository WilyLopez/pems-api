package com.playzone.pems.domain.finanzas.model;

import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroIngreso {
    private Long             id;
    private String           tipoIngresoCodigo;
    private Long             idSede;
    private Long             idReservaPublica;
    private Long             idEventoPrivado;
    private BigDecimal       monto;
    private LocalDate        fecha;
    private LocalDate        fechaCobro;
    private String           medioPago;
    private String           descripcion;
    private boolean          esAutomatico;
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;
    private Long             idRegistroAnulado;
    private UUID             idUsuarioRegistra;
    private OffsetDateTime    fechaCreacion;

    public boolean esContraasiento() {
        return naturaleza == NaturalezaMovimientoCaja.CONTRAASIENTO;
    }
}
