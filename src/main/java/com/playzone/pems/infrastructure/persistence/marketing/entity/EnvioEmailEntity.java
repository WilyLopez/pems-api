package com.playzone.pems.infrastructure.persistence.marketing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "envio_email")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnvioEmailEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "campana_id")
    private Long campanaId;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "plantilla_id")
    private Long plantillaId;

    @Column(name = "usuario_id", columnDefinition = "uuid")
    private UUID usuarioId;

    @Column(nullable = false, length = 120)
    private String destinatario;

    @Column(nullable = false, length = 200)
    private String asunto;

    @Column(name = "cuerpo_html", columnDefinition = "text")
    private String cuerpoHtml;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String estado = "PENDIENTE";

    @Column(nullable = false)
    @Builder.Default
    private int intentos = 0;

    @Column(name = "enviado_at")
    private OffsetDateTime enviadoAt;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "proveedor_mensaje_id", length = 200)
    private String proveedorMensajeId;

    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
