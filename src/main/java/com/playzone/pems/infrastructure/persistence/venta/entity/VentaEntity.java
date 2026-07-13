package com.playzone.pems.infrastructure.persistence.venta.entity;

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
@Table(name = "venta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VentaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private SedeEntity sede;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "evento_id")
    private Long eventoId;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "canal_codigo", nullable = false)
    private String canalCodigo;

    @Column(name = "fecha_visita")
    private LocalDate fechaVisita;

    @Column(name = "nombre_acompanante")
    private String nombreAcompanante;

    @Column(name = "dni_acompanante")
    private String dniAcompanante;

    @Column(name = "tipo_documento_acompanante", length = 3)
    private String tipoDocumentoAcompanante;

    @Column(name = "telefono_acompanante")
    private String telefonoAcompanante;

    @Column(name = "promocion_id")
    private Long promocionId;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "descuento", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal descuento = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "efectivo_recibido", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal efectivoRecibido = BigDecimal.ZERO;

    @Column(name = "vuelto", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal vuelto = BigDecimal.ZERO;

    @Column(name = "acta_firmada", nullable = false)
    @Builder.Default
    private boolean actaFirmada = false;

    @Column(name = "es_anticipada", nullable = false)
    @Builder.Default
    private boolean esAnticipada = false;

    @Column(name = "impreso", nullable = false)
    @Builder.Default
    private boolean impreso = false;

    @Column(name = "enviado_correo", nullable = false)
    @Builder.Default
    private boolean enviadoCorreo = false;

    @Column(name = "descargado", nullable = false)
    @Builder.Default
    private boolean descargado = false;

    @Column(name = "notas")
    private String notas;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", nullable = false, columnDefinition = "uuid")
    private UUID createdBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
