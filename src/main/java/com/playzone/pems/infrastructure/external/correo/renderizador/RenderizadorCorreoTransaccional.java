package com.playzone.pems.infrastructure.external.correo.renderizador;

import com.playzone.pems.domain.notificacion.model.Notificacion;

public interface RenderizadorCorreoTransaccional {

    String tipoCodigo();

    ContenidoCorreo renderizar(Notificacion notificacion);
}
