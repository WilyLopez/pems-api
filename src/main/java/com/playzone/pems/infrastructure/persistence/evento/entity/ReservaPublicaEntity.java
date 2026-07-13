package com.playzone.pems.infrastructure.persistence.evento.entity;

import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.evento.model.enums.CanalReserva;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reserva")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservaPublicaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "cliente_id")
    private Long clienteId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_codigo", nullable = false, length = 40)
    private EstadoReservaPublica estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_codigo", nullable = false, length = 30)
    private CanalReserva canalReserva;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dia_codigo", nullable = false, length = 30)
    private TipoDia tipoDia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reprogramada_desde_id")
    private ReservaPublicaEntity reservaOriginal;

    @Column(name = "es_reprogramacion", nullable = false)
    private boolean esReprogramacion = false;

    @Column(name = "veces_reprogramada", nullable = false)
    private int vecesReprogramada = 0;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Generated(GenerationTime.INSERT)
    @Column(name = "numero_ticket", insertable = false, updatable = false, length = 50)
    private String numeroTicket;

    @Column(name = "precio_historico", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHistorico;

    @Column(name = "descuento_aplicado", nullable = false, precision = 10, scale = 2)
    private BigDecimal descuentoAplicado = BigDecimal.ZERO;

    @Column(name = "total_pagado", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPagado;

    @Column(name = "nombre_nino", nullable = false, length = 120)
    private String nombreNino;

    @Column(name = "edad_nino", nullable = false)
    private int edadNino;

    @Column(name = "nombre_acompanante", length = 120)
    private String nombreAcompanante;

    @Column(name = "dni_acompanante", length = 15)
    private String dniAcompanante;

    @Column(name = "tipo_documento_acompanante", length = 3)
    private String tipoDocumentoAcompanante;

    @Column(name = "firmo_consentimiento", nullable = false)
    private boolean firmoConsentimiento = false;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @Column(name = "ingresado", nullable = false)
    @Builder.Default
    private boolean ingresado = false;

    @Column(name = "ingreso_at")
    private OffsetDateTime ingresoAt;

    @Generated(GenerationTime.INSERT)
    @Column(name = "codigo_qr", insertable = false, updatable = false, length = 200)
    private String codigoQr;

    @Column(name = "venta_id")
    private Long ventaId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
