package com.playzone.pems.infrastructure.persistence.finanzas.adapter;

import com.playzone.pems.domain.finanzas.model.RegistroIngreso;
import com.playzone.pems.domain.finanzas.query.MontoPorDia;
import com.playzone.pems.domain.finanzas.repository.RegistroIngresoRepository;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.RegistroIngresoEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.jpa.RegistroIngresoJpaRepository;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RegistroIngresoPersistenceAdapter implements RegistroIngresoRepository {

    private final RegistroIngresoJpaRepository jpaRepository;
    private final SedeJpaRepository            sedeJpaRepository;

    @Override
    public Optional<RegistroIngreso> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsAnuladoPara(Long idRegistroOriginal) {
        return jpaRepository.existsByRegistroAnuladoId(idRegistroOriginal);
    }

    @Override
    public List<RegistroIngreso> findTesoreriaWeb(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.findTesoreriaWeb(idSede, inicio, fin)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public Page<RegistroIngreso> findBySede(Long idSede, Pageable pageable) {
        return jpaRepository.findBySede_IdWithTipo(idSede, pageable).map(this::toDomain);
    }

    @Override
    public List<RegistroIngreso> findBySedeAndRangoFecha(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.findBySede_IdAndFechaBetweenWithTipo(idSede, inicio, fin)
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<RegistroIngreso> findBySedeAndPeriodo(Long idSede, int anio, int mes) {
        return jpaRepository.findBySede_IdAndPeriodoWithTipo(idSede, anio, mes)
                .stream().map(this::toDomain).toList();
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
    public BigDecimal sumMontoBySedeAndRangoCobro(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.sumMontoBySedeAndRangoCobro(idSede, inicio, fin);
    }

    @Override
    public Map<String, BigDecimal> sumMontoAgrupadoPorTipo(Long idSede, int anio, int mes) {
        return jpaRepository.sumMontoAgrupadoPorTipo(idSede, anio, mes).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));
    }

    @Override
    public List<MontoPorDia> sumMontoAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin) {
        return jpaRepository.sumMontoAgrupadoPorDia(idSede, inicio, fin).stream()
                .map(row -> new MontoPorDia((LocalDate) row[0], (BigDecimal) row[1]))
                .toList();
    }

    @Override
    @Transactional
    public RegistroIngreso save(RegistroIngreso ingreso) {
        SedeEntity sede = sedeJpaRepository.getReferenceById(ingreso.getIdSede());
        RegistroIngresoEntity entity = RegistroIngresoEntity.builder()
                .id(ingreso.getId())
                .tipoCodigo(ingreso.getTipoIngresoCodigo())
                .sede(sede)
                .reservaId(ingreso.getIdReservaPublica())
                .eventoId(ingreso.getIdEventoPrivado())
                .monto(ingreso.getMonto())
                .fecha(ingreso.getFecha())
                .fechaCobro(ingreso.getFechaCobro())
                .medioPagoCodigo(ingreso.getMedioPago())
                .descripcion(ingreso.getDescripcion())
                .esAutomatico(ingreso.isEsAutomatico())
                .naturaleza(ingreso.getNaturaleza())
                .registroAnuladoId(ingreso.getIdRegistroAnulado())
                .createdBy(ingreso.getIdUsuarioRegistra())
                .build();
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        jpaRepository.deleteById(id);
    }

    private RegistroIngreso toDomain(RegistroIngresoEntity e) {
        return RegistroIngreso.builder()
                .id(e.getId())
                .tipoIngresoCodigo(e.getTipoCodigo())
                .idSede(e.getSede().getId())
                .idReservaPublica(e.getReservaId())
                .idEventoPrivado(e.getEventoId())
                .monto(e.getMonto())
                .fecha(e.getFecha())
                .fechaCobro(e.getFechaCobro())
                .medioPago(e.getMedioPagoCodigo())
                .descripcion(e.getDescripcion())
                .esAutomatico(e.isEsAutomatico())
                .naturaleza(e.getNaturaleza())
                .idRegistroAnulado(e.getRegistroAnuladoId())
                .idUsuarioRegistra(e.getCreatedBy())
                .fechaCreacion(e.getCreatedAt() != null ? e.getCreatedAt() : null)
                .build();
    }
}
