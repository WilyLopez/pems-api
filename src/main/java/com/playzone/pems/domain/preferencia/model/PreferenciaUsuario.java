package com.playzone.pems.domain.preferencia.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PreferenciaUsuario {

    private UUID           usuarioId;

    private String         tema;
    private String         tipografia;
    private String         tamanioFuente;

    private boolean        sonidoNotificaciones;
    private boolean        notificacionesPush;
    private boolean        notificacionesEmail;
    private boolean        notificacionesVisuales;
    private boolean        badgesDinamicos;

    private OffsetDateTime fechaCreacion;
    private OffsetDateTime fechaActualizacion;
}
