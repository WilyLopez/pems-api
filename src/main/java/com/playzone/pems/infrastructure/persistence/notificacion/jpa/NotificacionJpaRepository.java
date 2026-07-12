package com.playzone.pems.infrastructure.persistence.notificacion.jpa;

import com.playzone.pems.infrastructure.persistence.notificacion.entity.NotificacionEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface NotificacionJpaRepository extends JpaRepository<NotificacionEntity, Long> {

    Optional<NotificacionEntity> findFirstByEntidadTipoAndEntidadIdOrderByFechaCreacionDesc(
            String entidadTipo, Long entidadId);

    @Query("""
            SELECT n FROM NotificacionEntity n
            WHERE n.destinatarioUsuario.id = :usuarioId
              AND (:soloNoLeidas = FALSE OR n.leida = FALSE)
              AND (n.fechaExpiracion IS NULL OR n.fechaExpiracion > CURRENT_TIMESTAMP)
            ORDER BY n.leida ASC, n.fechaCreacion DESC
            """)
    Page<NotificacionEntity> findFeedUsuario(
            @Param("usuarioId")     UUID    usuarioId,
            @Param("soloNoLeidas")  boolean soloNoLeidas,
            Pageable pageable);

    @Query("""
            SELECT n FROM NotificacionEntity n
            WHERE n.destinatarioCliente.id = :clienteId
              AND (:soloNoLeidas = FALSE OR n.leida = FALSE)
              AND (n.fechaExpiracion IS NULL OR n.fechaExpiracion > CURRENT_TIMESTAMP)
            ORDER BY n.leida ASC, n.fechaCreacion DESC
            """)
    Page<NotificacionEntity> findFeedCliente(
            @Param("clienteId")    Long    clienteId,
            @Param("soloNoLeidas") boolean soloNoLeidas,
            Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM NotificacionEntity n
            WHERE n.destinatarioUsuario.id = :usuarioId
              AND n.leida = FALSE
              AND (n.fechaExpiracion IS NULL OR n.fechaExpiracion > CURRENT_TIMESTAMP)
            """)
    long countNoLeidasUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT COUNT(n) FROM NotificacionEntity n
            WHERE n.destinatarioCliente.id = :clienteId
              AND n.leida = FALSE
              AND (n.fechaExpiracion IS NULL OR n.fechaExpiracion > CURRENT_TIMESTAMP)
            """)
    long countNoLeidasCliente(@Param("clienteId") Long clienteId);

    @Modifying
    @Query("""
            UPDATE NotificacionEntity n
            SET n.leida = TRUE, n.fechaLectura = CURRENT_TIMESTAMP
            WHERE n.id = :id
              AND n.destinatarioUsuario.id = :usuarioId
              AND n.leida = FALSE
            """)
    void marcarLeidaUsuario(@Param("id") Long id, @Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("""
            UPDATE NotificacionEntity n
            SET n.leida = TRUE, n.fechaLectura = CURRENT_TIMESTAMP
            WHERE n.id = :id
              AND n.destinatarioCliente.id = :clienteId
              AND n.leida = FALSE
            """)
    void marcarLeidaCliente(@Param("id") Long id, @Param("clienteId") Long clienteId);

    @Modifying
    @Query("""
            UPDATE NotificacionEntity n
            SET n.leida = TRUE, n.fechaLectura = CURRENT_TIMESTAMP
            WHERE n.destinatarioUsuario.id = :usuarioId
              AND n.leida = FALSE
            """)
    void marcarTodasLeidasUsuario(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("""
            UPDATE NotificacionEntity n
            SET n.leida = TRUE, n.fechaLectura = CURRENT_TIMESTAMP
            WHERE n.destinatarioCliente.id = :clienteId
              AND n.leida = FALSE
            """)
    void marcarTodasLeidasCliente(@Param("clienteId") Long clienteId);
}
