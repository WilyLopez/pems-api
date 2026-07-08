package com.playzone.pems.infrastructure.persistence.finanzas.entity;

import com.playzone.pems.domain.finanzas.model.enums.CategoriaRetiro;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoMovimientoCaja;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "movimiento_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimientoCajaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    private SesionCajaEntity sesionCaja;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimientoCaja tipo;

    @Column(name = "concepto", nullable = false)
    private String concepto;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "medio_pago_codigo", length = 30)
    private String medioPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_retiro", length = 30)
    private CategoriaRetiro categoriaRetiro;

    @Column(name = "registro_ingreso_id")
    private Long idRegistroIngreso;

    @Column(name = "registro_egreso_id")
    private Long idRegistroEgreso;

    @Column(name = "venta_id", updatable = false)
    private Long ventaId;

    @Column(name = "es_manual", nullable = false)
    @Builder.Default
    private boolean esManual = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza", nullable = false, length = 20)
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;

    @Column(name = "movimiento_anulado_id", updatable = false)
    private Long movimientoAnuladoId;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
