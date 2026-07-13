package com.playzone.pems.infrastructure.persistence.venta.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "venta_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaPagoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "venta_id", nullable = false)
    private Long ventaId;

    @Column(name = "medio_pago_codigo", nullable = false)
    private String medioPagoCodigo;

    @Column(name = "monto", nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "es_validado", nullable = false)
    @Builder.Default
    private boolean esValidado = true;

    @Column(name = "validado_por", columnDefinition = "uuid")
    private UUID validadoPor;

    @Column(name = "validado_at")
    private OffsetDateTime validadoAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
