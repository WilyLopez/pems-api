package com.playzone.pems.infrastructure.persistence.usuario_supabase.jpa;

import com.playzone.pems.infrastructure.persistence.usuario_supabase.entity.PerfilUsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PerfilUsuarioJpaRepository extends JpaRepository<PerfilUsuarioEntity, UUID> {

    Optional<PerfilUsuarioEntity> findByCorreoAndDeletedAtIsNull(String correo);

    Optional<PerfilUsuarioEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<PerfilUsuarioEntity> findByIdInAndDeletedAtIsNull(List<UUID> ids);

    @Modifying
    @Query("UPDATE PerfilUsuarioEntity p SET p.nombreCompleto = :nombre, p.telefono = :telefono WHERE p.id = :id")
    void actualizarNombreYTelefono(@Param("id") UUID id,
                                   @Param("nombre") String nombre,
                                   @Param("telefono") String telefono);
}
