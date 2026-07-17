package com.playzone.pems.infrastructure.persistence.comercial.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "servicio_imagen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioImagenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioCotizacionEntity servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variante_id")
    private ServicioVarianteEntity variante;

    @Column(name = "archivo_path", nullable = false, columnDefinition = "TEXT")
    private String archivoPath;

    @Column(name = "alt_texto", columnDefinition = "TEXT")
    private String altTexto;

    @Column(nullable = false)
    private int orden;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
