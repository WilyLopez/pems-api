package com.playzone.pems.infrastructure.persistence.evento.jpa;

import com.playzone.pems.infrastructure.persistence.evento.entity.EventoServicioEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventoServicioJpaRepository extends JpaRepository<EventoServicioEntity, Long> {

    List<EventoServicioEntity> findByEvento_Id(Long idEvento);

    @Modifying
    @Query("DELETE FROM EventoServicioEntity s WHERE s.evento.id = :idEvento")
    void deleteByEvento_Id(@Param("idEvento") Long idEvento);
}
