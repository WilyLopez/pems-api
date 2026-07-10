package com.playzone.pems.application.preferencia.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PreferenciaAdminResponse {

    private String  id;
    private String  idUsuarioAdmin;

    private String  tema;
    private String  tipografia;
    private String  tamanioFuente;

    private boolean sonidoNotificaciones;
    private boolean notificacionesPush;
    private boolean notificacionesEmail;
    private boolean notificacionesVisuales;
    private boolean badgesDinamicos;

    private String  fechaCreacion;
    private String  fechaActualizacion;
}
