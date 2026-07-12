package com.playzone.pems.domain.usuario.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class StaffToken {

    private Long id;
    private UUID usuarioId;
    private String tokenHash;
    private String tipo;
    private OffsetDateTime expiraAt;
    private OffsetDateTime usadoAt;
    private OffsetDateTime createdAt;

    public boolean estaVigente() {
        return usadoAt == null && expiraAt.isAfter(OffsetDateTime.now());
    }
}
