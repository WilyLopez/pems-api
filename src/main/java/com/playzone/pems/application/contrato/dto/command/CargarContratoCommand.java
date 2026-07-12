package com.playzone.pems.application.contrato.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CargarContratoCommand {
    private Long   idEventoPrivado;
    private byte[] archivo;
    private String contentType;
    private UUID   idUsuarioCarga;
}
