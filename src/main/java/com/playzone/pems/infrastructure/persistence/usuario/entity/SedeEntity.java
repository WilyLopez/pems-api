package com.playzone.pems.infrastructure.persistence.usuario.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sede")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SedeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 80)
    private String ciudad;

    @Column(nullable = false, length = 80)
    private String departamento;

    @Column(length = 11)
    private String ruc;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "google_maps_embed_url")
    private String googleMapsEmbedUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}
