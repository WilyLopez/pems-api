package com.playzone.pems.domain.notificacion.repository;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository {

    Optional<Notificacion> findById(Long id);

    Optional<Notificacion> findUltimaPorEntidad(String entidadTipo, Long entidadId);

    Page<Notificacion> findFeedUsuario(UUID usuarioId, boolean soloNoLeidas, Pageable pageable);

    Page<Notificacion> findFeedCliente(Long clienteId, boolean soloNoLeidas, Pageable pageable);

    long contarNoLeidasUsuario(UUID usuarioId);

    long contarNoLeidasCliente(Long clienteId);

    void marcarLeidaUsuario(Long id, UUID usuarioId);

    void marcarLeidaCliente(Long id, Long clienteId);

    void marcarTodasLeidasUsuario(UUID usuarioId);

    void marcarTodasLeidasCliente(Long clienteId);

    Notificacion save(Notificacion notificacion);

    List<Notificacion> saveAll(List<Notificacion> notificaciones);
}
