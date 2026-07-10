package com.playzone.pems.infrastructure.persistence.finanzas.adapter;

import com.playzone.pems.domain.finanzas.model.GastoOperativoDiario;
import com.playzone.pems.domain.finanzas.query.GastosPorDia;
import com.playzone.pems.domain.finanzas.repository.GastoOperativoDiarioRepository;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.GastoOperativoDiarioEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.jpa.GastoOperativoDiarioJpaRepository;
import com.playzone.pems.infrastructure.persistence.finanzas.mapper.FinanzasEntityMapper;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GastoOperativoDiarioPersistenceAdapter implements GastoOperativoDiarioRepository {

    private final GastoOperativoDiarioJpaRepository jpaRepository;
    private final SedeJpaRepository                 sedeJpaRepository;
    private final FinanzasEntityMapper              mapper;

    @Override
    public Optional<GastoOperativoDiario> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsAnuladoPara(Long idGastoOriginal) {
        return jpaRepository.existsByGastoAnuladoId(idGastoOriginal);
    }

    @Override
    public List<GastoOperativoDiario> findBySedeAndFecha(Long idSede, LocalDate fecha) {
        return jpaRepository.findBySede_IdAndFecha(idSede, fecha).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<GastoOperativoDiario> findBySedeAndRangoFecha(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.findBySede_IdAndFechaBetween(idSede, inicio, fin).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public BigDecimal sumMontoBySedeAndFecha(Long idSede, LocalDate fecha) {
        return jpaRepository.sumMontoBySedeAndFecha(idSede, fecha);
    }

    @Override
    public BigDecimal sumMontoBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return jpaRepository.sumMontoBySedeAndPeriodo(idSede, anio, mes);
    }

    @Override
    public BigDecimal sumMontoBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.sumMontoBySedeAndRango(idSede, inicio, fin);
    }

    @Override
    public List<GastosPorDia> sumMontoAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.sumMontoAgrupadoPorDia(idSede, inicio, fin);
    }

    @Override
    @Transactional
    public GastoOperativoDiario save(GastoOperativoDiario gasto) {
        SedeEntity sede = sedeJpaRepository.getReferenceById(gasto.getIdSede());
        GastoOperativoDiarioEntity entity = GastoOperativoDiarioEntity.builder()
                .id(gasto.getId())
                .sede(sede)
                .fecha(gasto.getFecha())
                .descripcion(gasto.getDescripcion())
                .monto(gasto.getMonto())
                .comprobantePath(gasto.getComprobanteUrl())
                .naturaleza(gasto.getNaturaleza())
                .gastoAnuladoId(gasto.getIdGastoAnulado())
                .createdBy(gasto.getIdUsuarioRegistra())
                .build();
        return mapper.toDomain(jpaRepository.save(entity));
    }
}
