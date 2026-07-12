package com.playzone.pems.domain.notificacion.repository;

import com.playzone.pems.domain.notificacion.model.NotificacionEntrega;

import java.util.List;

public interface NotificacionEntregaRepository {

    NotificacionEntrega save(NotificacionEntrega entrega);

    List<NotificacionEntrega> saveAll(List<NotificacionEntrega> entregas);

    List<NotificacionEntrega> findPendientesEmail(int limite);

    List<NotificacionEntrega> findParaReintentarEmail(int maxIntentos, int limite);

    List<NotificacionEntrega> findByNotificacionId(Long notificacionId);
}
