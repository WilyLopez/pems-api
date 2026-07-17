package com.playzone.pems.infrastructure.persistence.comercial.jpa;

import com.playzone.pems.infrastructure.persistence.comercial.entity.ServicioImagenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServicioImagenJpaRepository extends JpaRepository<ServicioImagenEntity, Long> {
    List<ServicioImagenEntity> findByServicio_IdOrderByOrdenAsc(Long idServicio);
    List<ServicioImagenEntity> findByServicio_IdInOrderByOrdenAsc(List<Long> idsServicio);
    long countByServicio_IdAndVarianteIsNull(Long idServicio);
    long countByVariante_Id(Long idVariante);

    @Modifying
    @Query("UPDATE ServicioImagenEntity i SET i.esPrincipal = FALSE " +
            "WHERE i.servicio.id = :idServicio AND ((:idVariante IS NULL AND i.variante IS NULL) OR i.variante.id = :idVariante)")
    void limpiarPrincipal(@Param("idServicio") Long idServicio, @Param("idVariante") Long idVariante);
}
