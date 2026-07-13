package com.playzone.pems.infrastructure.persistence.notificacion.adapter;

import com.playzone.pems.domain.notificacion.model.Notificacion;
import com.playzone.pems.domain.notificacion.model.TipoNotificacion;
import com.playzone.pems.domain.notificacion.repository.NotificacionRepository;
import com.playzone.pems.domain.notificacion.repository.TipoNotificacionRepository;
import com.playzone.pems.infrastructure.persistence.notificacion.entity.NotificacionEntity;
import com.playzone.pems.infrastructure.persistence.notificacion.entity.TipoNotificacionEntity;
import com.playzone.pems.infrastructure.persistence.notificacion.jpa.NotificacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.notificacion.jpa.TipoNotificacionJpaRepository;
import com.playzone.pems.infrastructure.persistence.notificacion.mapper.NotificacionEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.ClientePerfilEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.PerfilUsuarioEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.ClientePerfilJpaRepository;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa.PerfilUsuarioJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificacionPersistenceAdapter implements NotificacionRepository, TipoNotificacionRepository {

    private final NotificacionJpaRepository     jpa;
    private final TipoNotificacionJpaRepository tipoJpa;
    private final PerfilUsuarioJpaRepository    usuarioJpa;
    private final ClientePerfilJpaRepository    clienteJpa;
    private final NotificacionEntityMapper      mapper;

    @Override
    public Optional<Notificacion> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Notificacion> findUltimaPorEntidad(String entidadTipo, Long entidadId) {
        return jpa.findFirstByEntidadTipoAndEntidadIdOrderByFechaCreacionDesc(entidadTipo, entidadId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<Notificacion> findFeedUsuario(UUID usuarioId, boolean soloNoLeidas, Pageable pageable) {
        return jpa.findFeedUsuario(usuarioId, soloNoLeidas, pageable).map(mapper::toDomain);
    }

    @Override
    public Page<Notificacion> findFeedCliente(Long clienteId, boolean soloNoLeidas, Pageable pageable) {
        return jpa.findFeedCliente(clienteId, soloNoLeidas, pageable).map(mapper::toDomain);
    }

    @Override
    public long contarNoLeidasUsuario(UUID usuarioId) {
        return jpa.countNoLeidasUsuario(usuarioId);
    }

    @Override
    public long contarNoLeidasCliente(Long clienteId) {
        return jpa.countNoLeidasCliente(clienteId);
    }

    @Override
    @Transactional
    public void marcarLeidaUsuario(Long id, UUID usuarioId) {
        jpa.marcarLeidaUsuario(id, usuarioId);
    }

    @Override
    @Transactional
    public void marcarLeidaCliente(Long id, Long clienteId) {
        jpa.marcarLeidaCliente(id, clienteId);
    }

    @Override
    @Transactional
    public void marcarTodasLeidasUsuario(UUID usuarioId) {
        jpa.marcarTodasLeidasUsuario(usuarioId);
    }

    @Override
    @Transactional
    public void marcarTodasLeidasCliente(Long clienteId) {
        jpa.marcarTodasLeidasCliente(clienteId);
    }

    @Override
    @Transactional
    public void eliminarCliente(Long id, Long clienteId) {
        jpa.eliminarCliente(id, clienteId);
    }

    @Override
    @Transactional
    public Notificacion save(Notificacion notificacion) {
        return mapper.toDomain(jpa.save(buildEntity(notificacion)));
    }

    @Override
    @Transactional
    public List<Notificacion> saveAll(List<Notificacion> notificaciones) {
        var entities = notificaciones.stream().map(this::buildEntity).toList();
        return jpa.saveAll(entities).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<TipoNotificacion> findByCodigo(String codigo) {
        return tipoJpa.findById(codigo).map(this::mapTipo);
    }

    private TipoNotificacion mapTipo(TipoNotificacionEntity e) {
        return TipoNotificacion.builder()
                .codigo(e.getCodigo())
                .modulo(e.getModulo())
                .nombre(e.getNombre())
                .descripcion(e.getDescripcion())
                .destinatarioDefault(e.getDestinatarioDefault())
                .canalesDefault(e.getCanalesDefault())
                .plantillaTitulo(e.getPlantillaTitulo())
                .plantillaMensaje(e.getPlantillaMensaje())
                .prioridad(e.getPrioridad())
                .esObligatoria(e.isEsObligatoria())
                .esSistema(e.isEsSistema())
                .activo(e.isActivo())
                .orden(e.getOrden())
                .build();
    }

    private NotificacionEntity buildEntity(Notificacion n) {
        TipoNotificacionEntity tipo = tipoJpa.findById(n.getTipoCodigo())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TipoNotificacion", "codigo", n.getTipoCodigo()));

        PerfilUsuarioEntity usuario = n.getDestinatarioUsuarioId() != null
                ? usuarioJpa.getReferenceById(n.getDestinatarioUsuarioId())
                : null;

        ClientePerfilEntity cliente = n.getDestinatarioClienteId() != null
                ? clienteJpa.getReferenceById(n.getDestinatarioClienteId())
                : null;

        return mapper.toEntity(n, tipo, usuario, cliente);
    }
}
