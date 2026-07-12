package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.StaffPerfilEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffPerfilJpaRepository extends JpaRepository<StaffPerfilEntity, Long> {

    Optional<StaffPerfilEntity> findByUsuarioIdAndDeletedAtIsNull(UUID usuarioId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT s FROM StaffPerfilEntity s
            JOIN PerfilUsuarioEntity p ON s.usuarioId = p.id
            WHERE p.correo = :correo AND s.deletedAt IS NULL
            """)
    Optional<StaffPerfilEntity> findByCorreo(@org.springframework.data.repository.query.Param("correo") String correo);

    Optional<StaffPerfilEntity> findByIdAndDeletedAtIsNull(Long id);

    java.util.List<StaffPerfilEntity> findByDeletedAtIsNull();

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COUNT(sp.id)
            FROM staff_perfil sp
            INNER JOIN usuario_rol ur ON sp.usuario_id = ur.usuario_id
            WHERE ur.rol_codigo = :rolCodigo
              AND sp.es_activo = true
              AND sp.deleted_at IS NULL
            """, nativeQuery = true)
    long contarActivosPorRol(@org.springframework.data.repository.query.Param("rolCodigo") String rolCodigo);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COUNT(sp.id)
            FROM staff_perfil sp
            INNER JOIN usuario_rol ur ON sp.usuario_id = ur.usuario_id
            WHERE ur.rol_codigo = :rolCodigo
              AND sp.es_activo = true
              AND sp.deleted_at IS NULL
              AND sp.id <> :excludeId
            """, nativeQuery = true)
    long contarActivosPorRolExcluyendo(
            @org.springframework.data.repository.query.Param("rolCodigo") String rolCodigo,
            @org.springframework.data.repository.query.Param("excludeId") Long excludeId);

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT DISTINCT sp.usuario_id
            FROM staff_perfil sp
            INNER JOIN usuario_rol ur ON sp.usuario_id = ur.usuario_id
            WHERE ur.rol_codigo IN ('SUPERADMIN', 'ADMIN')
              AND sp.es_activo = true
              AND sp.deleted_at IS NULL
            """, nativeQuery = true)
    java.util.List<UUID> obtenerAdministradoresActivos();
}
