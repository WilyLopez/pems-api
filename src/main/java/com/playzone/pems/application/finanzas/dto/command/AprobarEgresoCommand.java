package com.playzone.pems.application.finanzas.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AprobarEgresoCommand {
    private final Long idEgreso;
    private final UUID idUsuarioAprueba;
    private final boolean esAdmin;
}
