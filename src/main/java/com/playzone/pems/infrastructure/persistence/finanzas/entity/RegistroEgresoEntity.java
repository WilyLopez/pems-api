package com.playzone.pems.infrastructure.persistence.finanzas.entity;

import com.playzone.pems.domain.finanzas.model.enums.EstadoAprobacionEgreso;
import com.playzone.pems.domain.finanzas.model.enums.NaturalezaMovimientoCaja;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registro_egreso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroEgresoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo_codigo", nullable = false, length = 50)
    private String tipoCodigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "periodo_anio")
    private Integer periodoAnio;

    @Column(name = "periodo_mes")
    private Integer periodoMes;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "comprobante_path", length = 500)
    private String comprobantePath;

    @Column(name = "medio_pago_codigo", length = 30)
    private String medioPagoCodigo;

    @Column(name = "es_recurrente", nullable = false)
    @Builder.Default
    private boolean esRecurrente = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza", nullable = false, length = 20)
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;

    @Column(name = "registro_anulado_id", updatable = false)
    private Long registroAnuladoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_aprobacion", nullable = false, length = 30)
    @Builder.Default
    private EstadoAprobacionEgreso estadoAprobacion = EstadoAprobacionEgreso.APROBADO;

    @Column(name = "aprobado_por", columnDefinition = "uuid")
    private UUID aprobadoPor;

    @Column(name = "fecha_aprobacion")
    private OffsetDateTime fechaAprobacion;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
