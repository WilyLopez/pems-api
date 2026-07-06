package com.playzone.pems.infrastructure.persistence.cms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "configuracion_publica")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionPublicaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre_negocio", nullable = false)
    private String nombreNegocio;

    @Column(name = "slogan")
    private String slogan;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "favicon_path")
    private String faviconPath;

    @Column(name = "logo_secundario_path")
    private String logoSecundarioPath;

    @Column(name = "mascota_1_path")
    private String mascota1Path;

    @Column(name = "mascota_2_path")
    private String mascota2Path;

    @Column(name = "telefono")
    private String telefono;

    @Column(name = "telefono_secundario")
    private String telefonoSecundario;

    @Column(name = "whatsapp")
    private String whatsapp;

    @Column(name = "correo")
    private String correo;

    @Column(name = "correo_secundario")
    private String correoSecundario;

    @Column(name = "facebook_url")
    private String facebookUrl;

    @Column(name = "instagram_url")
    private String instagramUrl;

    @Column(name = "tiktok_url")
    private String tiktokUrl;

    @Column(name = "youtube_url")
    private String youtubeUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metricas_negocio", columnDefinition = "jsonb")
    private String metricasNegocio;

    @Column(name = "es_mantenimiento_activo", nullable = false)
    private boolean esMantenimientoActivo;

    @Column(name = "mensaje_mantenimiento")
    private String mensajeMantenimiento;

    @Column(name = "copyright_texto")
    private String copyrightTexto;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;
}
