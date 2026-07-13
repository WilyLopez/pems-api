package com.playzone.pems.infrastructure.persistence.usuario_supabase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cliente_perfil")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClientePerfilEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Setter
    @Column(name = "usuario_id", columnDefinition = "uuid")
    private UUID usuarioId;

    @Setter
    @Column(name = "tipo_documento_codigo", nullable = false)
    private String tipoDocumentoCodigo;

    @Setter
    @Column(name = "numero_documento")
    private String numeroDocumento;

    @Setter
    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Setter
    @Column(name = "apellido_paterno")
    private String apellidoPaterno;

    @Setter
    @Column(name = "apellido_materno")
    private String apellidoMaterno;

    @Column(name = "nombre_completo", insertable = false, updatable = false)
    private String nombreCompleto;

    @Setter
    @Column(name = "correo")
    private String correo;

    @Setter
    @Column(name = "telefono")
    private String telefono;

    @Setter
    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    @Setter
    @Column(name = "ruc", length = 11)
    private String ruc;

    @Setter
    @Column(name = "razon_social")
    private String razonSocial;

    @Setter
    @Column(name = "direccion_fiscal")
    private String direccionFiscal;

    @Setter
    @Column(name = "segmento_codigo", nullable = false)
    private String segmentoCodigo;

    @Setter
    @Column(name = "es_vip", nullable = false)
    private boolean esVip;

    @Setter
    @Column(name = "descuento_vip", precision = 5, scale = 2)
    private BigDecimal descuentoVip;

    @Setter
    @Column(name = "acepta_comunicaciones", nullable = false)
    private boolean aceptaComunicaciones;

    @Setter
    @Column(name = "foto_perfil_path")
    private String fotoPerfilPath;

    @Setter
    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @Setter
    @Column(name = "contador_visitas", nullable = false)
    private int contadorVisitas;

    @Setter
    @Column(name = "total_gastado", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGastado;

    @Setter
    @Column(name = "origen", nullable = false)
    private String origen;

    @Setter
    @Column(name = "ultima_visita_at")
    private OffsetDateTime ultimaVisitaAt;

    @org.hibernate.annotations.CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Setter
    @Column(name = "created_by", columnDefinition = "uuid")
    private UUID createdBy;

    @Setter
    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @Setter
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
