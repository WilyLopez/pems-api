package com.playzone.pems.infrastructure.persistence.calendario.adapter;

import com.playzone.pems.domain.calendario.model.Tarifa;
import com.playzone.pems.domain.calendario.model.enums.TipoDia;
import com.playzone.pems.domain.calendario.repository.TarifaRepository;
import com.playzone.pems.infrastructure.persistence.calendario.entity.TarifaEntity;
import com.playzone.pems.infrastructure.persistence.calendario.jpa.TarifaJpaRepository;
import com.playzone.pems.infrastructure.persistence.calendario.mapper.CalendarioEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TarifaPersistenceAdapter implements TarifaRepository {

    private final TarifaJpaRepository       tarifaJpa;
    private final SedeJpaRepository         sedeJpa;
    private final CalendarioEntityMapper    mapper;

    @Override
    public Optional<Tarifa> findById(Long id) {
        return tarifaJpa.findById(id).map(mapper::toDomain);
    }

    @org.springframework.cache.annotation.Cacheable(value = "tarifas", key = "{#idSede, #tipoDia, #fecha}")
    @Override
    public Optional<Tarifa> findVigenteBySedeAndTipoDiaAndFecha(Long idSede, TipoDia tipoDia, LocalDate fecha) {
        return tarifaJpa.findVigenteBySedeAndTipoDiaAndFecha(idSede, tipoDia, fecha).map(mapper::toDomain);
    }

    @Override
    public List<Tarifa> findActivasBySede(Long idSede) {
        return tarifaJpa.findBySede_IdAndActivoTrueOrderByVigenciaDesdeDesc(idSede)
                .stream().map(mapper::toDomain).toList();
    }

    @org.springframework.cache.annotation.CacheEvict(value = "tarifas", allEntries = true)
    @Override
    @Transactional
    public Tarifa save(Tarifa tarifa) {
        var sede = sedeJpa.findById(tarifa.getIdSede())
                .orElseThrow(() -> new ResourceNotFoundException("Sede", tarifa.getIdSede()));

        TarifaEntity entity = TarifaEntity.builder()
                .id(tarifa.getId())
                .sede(sede)
                .tipoDia(tarifa.getTipoDia())
                .precio(tarifa.getPrecio())
                .duracionMinutos(tarifa.getDuracionMinutos())
                .vigenciaDesde(tarifa.getVigenciaDesde())
                .vigenciaHasta(tarifa.getVigenciaHasta())
                .activo(tarifa.isActivo())
                .build();

        return mapper.toDomain(tarifaJpa.save(entity));
    }

    @org.springframework.cache.annotation.CacheEvict(value = "tarifas", allEntries = true)
    @Override
    @Transactional
    public void desactivarAnterioresBySedeAndTipoDia(Long idSede, TipoDia tipoDia) {
        tarifaJpa.desactivarAnterioresBySedeAndTipoDia(idSede, tipoDia);
    }
}