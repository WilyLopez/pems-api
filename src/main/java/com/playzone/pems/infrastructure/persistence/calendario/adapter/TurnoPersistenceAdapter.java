package com.playzone.pems.infrastructure.persistence.calendario.adapter;

import com.playzone.pems.domain.calendario.model.Turno;
import com.playzone.pems.domain.calendario.repository.TurnoRepository;
import com.playzone.pems.infrastructure.persistence.calendario.jpa.TurnoJpaRepository;
import com.playzone.pems.infrastructure.persistence.calendario.mapper.CalendarioEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TurnoPersistenceAdapter implements TurnoRepository {

    private final TurnoJpaRepository    turnoJpa;
    private final CalendarioEntityMapper mapper;

    @Override
    public Optional<Turno> findById(Long id) {
        if (id == null) return Optional.empty();
        return turnoJpa.buscarPorIdNumerico(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Turno> findByCodigo(String codigo) {
        return turnoJpa.findByCodigo(codigo).map(mapper::toDomain);
    }

    @Override
    public List<Turno> findAll() {
        return turnoJpa.findAll().stream().map(mapper::toDomain).toList();
    }
}