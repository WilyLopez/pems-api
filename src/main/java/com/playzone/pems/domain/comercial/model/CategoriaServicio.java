package com.playzone.pems.domain.comercial.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CategoriaServicio {
    private Long    id;
    private String  nombre;
    private int     orden;
    private boolean activo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
