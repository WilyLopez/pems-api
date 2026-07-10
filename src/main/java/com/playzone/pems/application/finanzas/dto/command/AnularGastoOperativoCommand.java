package com.playzone.pems.application.finanzas.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class AnularGastoOperativoCommand {
    private Long id;
    private String motivo;
    private UUID idUsuarioAnula;
}
