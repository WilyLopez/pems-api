package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.UsuarioRolEntity;
import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UsuarioRolJpaRepository extends JpaRepository<UsuarioRolEntity, UsuarioRolId> {

    @Query(value = """
            SELECT ur.rol_codigo
            FROM public.usuario_rol ur
            JOIN public.rol r ON r.codigo = ur.rol_codigo
            WHERE ur.usuario_id = :usuarioId
              AND r.activo = TRUE
            """, nativeQuery = true)
    List<String> findRolCodigosByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Query(value = """
            SELECT ur.usuario_id AS usuarioId, ur.rol_codigo AS rolCodigo
            FROM public.usuario_rol ur
            JOIN public.rol r ON r.codigo = ur.rol_codigo
            WHERE ur.usuario_id IN (:usuarioIds)
              AND r.activo = TRUE
            """, nativeQuery = true)
    List<RolPorUsuarioProjection> findRolCodigosByUsuarioIds(@Param("usuarioIds") List<UUID> usuarioIds);

    interface RolPorUsuarioProjection {
        UUID getUsuarioId();
        String getRolCodigo();
    }

    @Query(value = """
            SELECT rp.permiso_codigo
            FROM public.usuario_rol ur
            JOIN public.rol r ON r.codigo = ur.rol_codigo
            JOIN public.rol_permiso rp ON ur.rol_codigo = rp.rol_codigo
            JOIN public.permiso p ON p.codigo = rp.permiso_codigo
            WHERE ur.usuario_id = :usuarioId
              AND r.activo = TRUE
              AND p.activo = TRUE
            """, nativeQuery = true)
    List<String> findPermisoCodigosByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUsuarioIdAndRolCodigo(UUID usuarioId, String rolCodigo);
}
