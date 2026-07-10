package com.playzone.pems.application.finanzas.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnularEgresoCommand {
    private final Long   idEgreso;
    private final String motivo;
    private final UUID   idUsuarioAnula;
}
