package com.playzone.pems.infrastructure.persistence.evento.entity;

import com.playzone.pems.domain.evento.model.enums.EstadoEventoPrivado;
import com.playzone.pems.domain.evento.model.enums.ModalidadPago;
import com.playzone.pems.infrastructure.persistence.calendario.entity.TurnoEntity;
import com.playzone.pems.infrastructure.persistence.comercial.entity.TipoEventoEntity;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;

import java.util.UUID;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "evento",
        uniqueConstraints = @UniqueConstraint(columnNames = {"sede_id", "fecha_evento", "turno_codigo"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoPrivadoEntity {

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
    private EstadoEventoPrivado estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "turno_codigo", referencedColumnName = "codigo", nullable = false)
    private TurnoEntity turno;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDate fechaEvento;

    @Column(name = "tipo_evento_codigo", nullable = false, length = 200)
    private String tipoEvento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_evento_codigo", referencedColumnName = "codigo", insertable = false, updatable = false)
    private TipoEventoEntity tipoEventoRef;

    @Column(name = "contacto_adicional", length = 200)
    private String contactoAdicional;

    @Column(name = "origen_contacto", length = 30)
    private String origenContacto;

    @Column(name = "aforo_declarado")
    private Integer aforoDeclarado;

    @Column(name = "precio_contrato", precision = 10, scale = 2)
    private BigDecimal precioContrato;

    @Column(name = "monto_adelanto", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal montoAdelanto = BigDecimal.ZERO;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "notas_internas", columnDefinition = "TEXT")
    private String notasInternas;

    @Column(name = "nombre_nino", length = 120)
    private String nombreNino;

    @Column(name = "edad_cumple")
    private Integer edadCumple;

    @Column(name = "paquete_id")
    private Long paqueteId;

    @Column(name = "descripcion_personalizada", columnDefinition = "TEXT")
    private String descripcionPersonalizada;

    @Column(name = "presupuesto_estimado", precision = 10, scale = 2)
    private BigDecimal presupuestoEstimado;

    @Column(name = "es_cotizacion_personalizada", nullable = false)
    @Builder.Default
    private boolean esCotizacionPersonalizada = false;

    @Column(name = "usuario_gestor_id", columnDefinition = "uuid")
    private UUID usuarioGestorId;

    @Column(name = "estado_operativo", length = 40)
    private String estadoOperativo;

    @Column(name = "checklist_completo", nullable = false)
    @Builder.Default
    private boolean checklistCompleto = false;

    @Column(name = "hora_inicio_real")
    private OffsetDateTime horaInicioReal;

    @Column(name = "hora_fin_real")
    private OffsetDateTime horaFinReal;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidad_pago", nullable = false, length = 20)
    @Builder.Default
    private ModalidadPago modalidadPago = ModalidadPago.AL_CONTADO;

    @Column(name = "fecha_limite_pago")
    private LocalDate fechaLimitePago;
}
