package com.playzone.pems.infrastructure.persistence.evento.entity;

import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioCotizacionEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "evento_servicio")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoServicioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoPrivadoEntity evento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "servicio_cotizacion_id")
    private ServicioCotizacionEntity servicioCotizacion;

    @Column(name = "servicio_variante_id")
    private Long servicioVarianteId;

    @Column(name = "nombre_libre", columnDefinition = "TEXT")
    private String nombreLibre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(name = "precio_acordado")
    private BigDecimal precioAcordado;

    @Column(nullable = false)
    private boolean incluido;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
