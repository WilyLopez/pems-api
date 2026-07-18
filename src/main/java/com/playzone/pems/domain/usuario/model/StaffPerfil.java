package com.playzone.pems.domain.usuario.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerfil {

    private Long      id;
    private UUID      usuarioId;
    private Long      sedeId;
    private String    codigoEmpleado;
    private LocalDate fechaIngreso;
    private String    telefonoEmergencia;
    private String    observaciones;
    private boolean   esActivo;
    private boolean   debeCambiarContrasena;
    private Integer   intentosFallidos;
    private java.time.OffsetDateTime bloqueadoHasta;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime updatedAt;
    private java.util.UUID createdBy;
    private java.util.UUID updatedBy;
}
