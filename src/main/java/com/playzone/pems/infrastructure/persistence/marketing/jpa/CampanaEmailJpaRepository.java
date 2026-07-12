package com.playzone.pems.infrastructure.persistence.marketing.jpa;

import com.playzone.pems.infrastructure.persistence.marketing.entity.CampanaEmailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface CampanaEmailJpaRepository extends JpaRepository<CampanaEmailEntity, Long> {

    @Query("SELECT c FROM CampanaEmailEntity c WHERE c.estado = 'PROGRAMADA' AND c.fechaProgramada <= :ahora")
    List<CampanaEmailEntity> findProgramadasParaEnviar(@Param("ahora") OffsetDateTime ahora);

    List<CampanaEmailEntity> findByEstado(String estado);

    @Modifying
    @Query("UPDATE CampanaEmailEntity c SET c.estado = :estado WHERE c.id = :id")
    void actualizarEstado(@Param("id") Long id, @Param("estado") String estado);

    @Modifying
    @Query("UPDATE CampanaEmailEntity c SET c.totalEnviados = c.totalEnviados + :cantidad WHERE c.id = :id")
    void incrementarEnviados(@Param("id") Long id, @Param("cantidad") int cantidad);

    @Modifying
    @Query("UPDATE CampanaEmailEntity c SET c.totalFallidos = c.totalFallidos + :cantidad WHERE c.id = :id")
    void incrementarFallidos(@Param("id") Long id, @Param("cantidad") int cantidad);
}
