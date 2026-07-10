package com.playzone.pems.infrastructure.persistence.finanzas.entity;

import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sesion_caja")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionCajaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @Column(name = "usuario_id", nullable = false, columnDefinition = "uuid")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoSesionCaja tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_codigo", nullable = false, length = 20)
    @Builder.Default
    private EstadoCaja estado = EstadoCaja.ABIERTA;

    @Column(name = "saldo_inicial", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal saldoInicial = BigDecimal.ZERO;

    @Column(name = "total_ingresos", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalIngresos = BigDecimal.ZERO;

    @Column(name = "total_egresos", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalEgresos = BigDecimal.ZERO;

    @Column(name = "saldo_esperado", precision = 10, scale = 2)
    private BigDecimal saldoEsperado;

    @Column(name = "saldo_final", precision = 10, scale = 2)
    private BigDecimal saldoFinal;

    @Column(name = "diferencia", precision = 10, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "abierta_at", nullable = false)
    private OffsetDateTime fechaApertura;

    @Column(name = "cerrada_at")
    private OffsetDateTime fechaCierre;

    @Column(name = "cerrada_por", columnDefinition = "uuid")
    private UUID cerradaPor;

    @Column(name = "motivo_cierre")
    private String motivoCierre;

    @Column(name = "observaciones")
    private String observaciones;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
