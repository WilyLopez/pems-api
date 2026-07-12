package com.playzone.pems.infrastructure.persistence.contrato.entity;

import com.playzone.pems.infrastructure.persistence.evento.entity.EventoPrivadoEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GenerationType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "contrato_evento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private EventoPrivadoEntity eventoPrivado;

    @Column(name = "archivo_pdf_path", nullable = false, length = 500)
    private String archivoPdfUrl;

    @Column(name = "cargado_por", columnDefinition = "uuid", nullable = false)
    private UUID cargadoPor;

    @Column(name = "cargado_at", nullable = false)
    private OffsetDateTime cargadoAt;
}
