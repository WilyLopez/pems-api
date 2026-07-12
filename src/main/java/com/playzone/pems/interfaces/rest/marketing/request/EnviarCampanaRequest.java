package com.playzone.pems.interfaces.rest.marketing.request;

import lombok.Getter;

import java.util.Map;

@Getter
public class EnviarCampanaRequest {

    private Boolean soloVip;
    private Boolean soloFrecuentes;
    private Boolean soloNuevos;
    private Boolean soloInactivos;
    private Boolean soloCorporativos;
    private Boolean soloConAccesoWeb;
    private Boolean soloPresenciales;
    private Map<String, String> valoresVariables;
}
