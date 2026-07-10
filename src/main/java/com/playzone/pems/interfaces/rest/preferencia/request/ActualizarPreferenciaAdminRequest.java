package com.playzone.pems.interfaces.rest.preferencia.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ActualizarPreferenciaAdminRequest {

    private String  tema;
    private String  tipografia;
    private String  tamanioFuente;

    private boolean sonidoNotificaciones;
    private boolean notificacionesPush;
    private boolean notificacionesEmail;
    private boolean notificacionesVisuales;
    private boolean badgesDinamicos;
}
