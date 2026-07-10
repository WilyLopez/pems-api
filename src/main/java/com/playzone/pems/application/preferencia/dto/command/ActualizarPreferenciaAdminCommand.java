package com.playzone.pems.application.preferencia.dto.command;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ActualizarPreferenciaAdminCommand {

    private String  tema;
    private String  tipografia;
    private String  tamanioFuente;

    private boolean sonidoNotificaciones;
    private boolean notificacionesPush;
    private boolean notificacionesEmail;
    private boolean notificacionesVisuales;
    private boolean badgesDinamicos;
}
