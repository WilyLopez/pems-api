package com.playzone.pems.infrastructure.persistence.calendario.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "turno")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TurnoEntity {

    @Id
    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @Column(nullable = false, name = "nombre", length = 60)
    private String descripcion;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    public String getNombre() {
        return descripcion;
    }
}