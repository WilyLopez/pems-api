package com.playzone.pems.infrastructure.persistence.usuario_supabase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff_perfil")
@Getter
@NoArgsConstructor
public class StaffPerfilEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Setter
    @Column(name = "usuario_id", columnDefinition = "uuid", nullable = false)
    private UUID usuarioId;

    @Setter
    @Column(name = "sede_id", nullable = false)
    private Long sedeId;

    @Setter
    @Column(name = "codigo_empleado")
    private String codigoEmpleado;

    @Setter
    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Setter
    @Column(name = "telefono_emergencia")
    private String telefonoEmergencia;

    @Setter
    @Column(name = "observaciones")
    private String observaciones;

    @Setter
    @Column(name = "es_activo", nullable = false)
    private boolean esActivo;

    @Setter
    @Column(name = "debe_cambiar_contrasena", nullable = false)
    private boolean debeCambiarContrasena;

    @Setter
    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos;

    @Setter
    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

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
}
