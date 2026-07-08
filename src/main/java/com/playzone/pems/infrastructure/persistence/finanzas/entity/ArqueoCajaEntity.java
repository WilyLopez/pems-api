package com.playzone.pems.infrastructure.persistence.finanzas.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "arqueo_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArqueoCajaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sesion_caja_id", nullable = false)
    private SesionCajaEntity sesionCaja;

    @Column(name = "saldo_esperado", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoEsperado;

    @Column(name = "saldo_contado", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldoContado;

    @Column(name = "diferencia", nullable = false, precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "realizado_por", nullable = false, columnDefinition = "uuid")
    private UUID realizadoPor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;
}
