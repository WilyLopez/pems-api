package com.playzone.pems.domain.usuario.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ClienteToken {

    private Long id;
    private Long clienteId;
    private String tokenHash;
    private String tipo;
    private String metadata;
    private OffsetDateTime expiraAt;
    private OffsetDateTime usadoAt;
    private OffsetDateTime createdAt;

    public boolean estaVigente() {
        return usadoAt == null && expiraAt.isAfter(OffsetDateTime.now());
    }
}
