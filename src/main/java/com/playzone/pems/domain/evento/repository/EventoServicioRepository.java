package com.playzone.pems.domain.evento.repository;

import com.playzone.pems.domain.evento.model.EventoServicio;

import java.util.List;

public interface EventoServicioRepository {

    List<EventoServicio> findByEvento(Long idEventoPrivado);

    List<EventoServicio> saveAll(List<EventoServicio> servicios);

    void deleteByEvento(Long idEventoPrivado);
}
