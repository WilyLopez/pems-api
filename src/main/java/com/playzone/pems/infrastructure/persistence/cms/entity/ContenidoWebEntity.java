package com.playzone.pems.infrastructure.persistence.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contenido_web")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContenidoWebEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seccion_codigo", nullable = false)
    private String seccionCodigo;

    @Column(name = "tipo_contenido_codigo", nullable = false)
    private String tipoContenidoCodigo;

    @Column(nullable = false)
    private String clave;

    @Column(name = "valor_es", nullable = false)
    private String valorEs;

    @Column(name = "valor_en")
    private String valorEn;

    @Column(name = "imagen_path")
    private String imagenUrl;

    @Column(length = 200)
    private String descripcion;

    @Column(name = "orden", nullable = false)
    private int orden;

    @Column(name = "es_visible", nullable = false)
    private boolean visible;

    @Column(name = "version", nullable = false)
    private int version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadatos", columnDefinition = "jsonb")
    private String metadatos;

    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
