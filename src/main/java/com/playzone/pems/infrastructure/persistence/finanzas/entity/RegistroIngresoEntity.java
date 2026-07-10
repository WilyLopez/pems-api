package com.playzone.pems.infrastructure.persistence.finanzas.entity;

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
@Table(name = "registro_ingreso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegistroIngresoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tipo_codigo", nullable = false, length = 50)
    private String tipoCodigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "evento_id")
    private Long eventoId;

    @Column(name = "venta_id")
    private Long ventaId;

    @Column(name = "venta_pago_id")
    private Long ventaPagoId;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_cobro", nullable = false)
    private LocalDate fechaCobro;

    @Column(name = "medio_pago_codigo", length = 30)
    private String medioPagoCodigo;

    @Column(name = "referencia", length = 200)
    private String referencia;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "es_automatico", nullable = false)
    @Builder.Default
    private boolean esAutomatico = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "naturaleza", nullable = false, length = 20)
    @Builder.Default
    private NaturalezaMovimientoCaja naturaleza = NaturalezaMovimientoCaja.NORMAL;

    @Column(name = "registro_anulado_id", updatable = false)
    private Long registroAnuladoId;

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

    @Column(name = "deleted_by", columnDefinition = "uuid")
    private UUID deletedBy;
}
