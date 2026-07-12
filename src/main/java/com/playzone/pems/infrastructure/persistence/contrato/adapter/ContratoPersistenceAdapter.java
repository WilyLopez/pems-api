package com.playzone.pems.infrastructure.persistence.contrato.adapter;

import com.playzone.pems.domain.contrato.model.Contrato;
import com.playzone.pems.domain.contrato.repository.ContratoRepository;
import com.playzone.pems.infrastructure.persistence.contrato.jpa.ContratoJpaRepository;
import com.playzone.pems.infrastructure.persistence.contrato.mapper.ContratoEntityMapper;
import com.playzone.pems.infrastructure.persistence.evento.jpa.EventoPrivadoJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ContratoPersistenceAdapter implements ContratoRepository {

    private final ContratoJpaRepository      contratoJpa;
    private final EventoPrivadoJpaRepository eventoJpa;
    private final ContratoEntityMapper       mapper;

    @Override
    public Optional<Contrato> findById(Long id) {
        return contratoJpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Contrato> findByEventoPrivado(Long idEventoPrivado) {
        return contratoJpa.findFirstByEventoPrivado_IdOrderByIdDesc(idEventoPrivado).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Contrato save(Contrato contrato) {
        var evento = eventoJpa.findById(contrato.getIdEventoPrivado())
                .orElseThrow(() -> new ResourceNotFoundException("EventoPrivado", contrato.getIdEventoPrivado()));
        return mapper.toDomain(contratoJpa.save(mapper.toEntity(contrato, evento)));
    }

    @Override
    public Page<Contrato> buscarConFiltros(Long idSede, LocalDate fechaEvento, Pageable pageable) {
        return contratoJpa.buscarConFiltros(idSede, fechaEvento, pageable).map(mapper::toDomain);
    }
}
