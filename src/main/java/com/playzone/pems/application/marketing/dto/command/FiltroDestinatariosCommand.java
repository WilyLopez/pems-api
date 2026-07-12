package com.playzone.pems.application.marketing.dto.command;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class FiltroDestinatariosCommand {

    private final Boolean soloVip;
    private final Boolean soloFrecuentes;
    private final Boolean soloNuevos;
    private final Boolean soloInactivos;
    private final Boolean soloCorporativos;
    private final Boolean soloConAccesoWeb;
    private final Boolean soloPresenciales;
    private final Map<String, String> valoresVariables;
}
