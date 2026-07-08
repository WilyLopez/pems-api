package com.playzone.pems.infrastructure.persistence.finanzas.adapter;

import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.EstadoCaja;
import com.playzone.pems.domain.finanzas.repository.SesionCajaRepository;
import com.playzone.pems.infrastructure.persistence.finanzas.entity.SesionCajaEntity;
import com.playzone.pems.infrastructure.persistence.finanzas.jpa.SesionCajaJpaRepository;
import com.playzone.pems.infrastructure.persistence.usuario.entity.SedeEntity;
import com.playzone.pems.infrastructure.persistence.usuario.jpa.SedeJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SesionCajaPersistenceAdapter implements SesionCajaRepository {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final SesionCajaJpaRepository jpaRepository;
    private final SedeJpaRepository       sedeJpaRepository;

    @Override
    public Optional<SesionCaja> findById(Long id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<SesionCaja> findAbiertaByUsuario(UUID usuarioId) {
        return jpaRepository.findByUsuarioIdAndEstado(usuarioId, EstadoCaja.ABIERTA).map(this::toDomain);
    }

    @Override
    public Optional<SesionCaja> findAbiertaByUsuarioAndSede(UUID usuarioId, Long idSede) {
        return jpaRepository.findByUsuarioIdAndSede_IdAndEstado(usuarioId, idSede, EstadoCaja.ABIERTA)
                .map(this::toDomain);
    }

    @Override
    public Optional<SesionCaja> findByUsuarioAndSedeAndFecha(UUID usuarioId, Long idSede, LocalDate fecha) {
        OffsetDateTime desde = fecha.atStartOfDay(LIMA).toOffsetDateTime();
        OffsetDateTime hasta = fecha.plusDays(1).atStartOfDay(LIMA).toOffsetDateTime();
        return jpaRepository
                .findFirstByUsuarioIdAndSede_IdAndFechaAperturaBetweenOrderByFechaAperturaDesc(
                        usuarioId, idSede, desde, hasta)
                .map(this::toDomain);
    }

    @Override
    public boolean existsAbiertaBySede(Long idSede) {
        return jpaRepository.existsBySede_IdAndEstado(idSede, EstadoCaja.ABIERTA);
    }

    @Override
    public List<SesionCaja> findBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin) {
        OffsetDateTime desde = inicio.atStartOfDay(LIMA).toOffsetDateTime();
        OffsetDateTime hasta = fin.plusDays(1).atStartOfDay(LIMA).toOffsetDateTime();
        return jpaRepository.findBySede_IdAndFechaAperturaBetweenOrderByFechaAperturaDesc(idSede, desde, hasta)
                .stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional
    public SesionCaja save(SesionCaja sesion) {
        SedeEntity sede = sedeJpaRepository.getReferenceById(sesion.getIdSede());
        SesionCajaEntity entity;
        if (sesion.getId() != null) {
            entity = jpaRepository.findById(sesion.getId())
                    .orElseThrow(() -> new IllegalStateException("Sesion de caja no encontrada: " + sesion.getId()));
            entity.setEstado(sesion.getEstado());
            entity.setSaldoInicial(sesion.getSaldoInicial());
            entity.setTotalIngresos(sesion.getTotalIngresos());
            entity.setTotalEgresos(sesion.getTotalEgresos());
            entity.setSaldoEsperado(sesion.getSaldoEsperado());
            entity.setSaldoFinal(sesion.getSaldoFinal());
            entity.setDiferencia(sesion.getDiferencia());
            entity.setFechaCierre(sesion.getFechaCierre());
            entity.setCerradaPor(sesion.getCerradaPor());
            entity.setMotivoCierre(sesion.getMotivoCierre());
            entity.setObservaciones(sesion.getObservaciones());
        } else {
            entity = SesionCajaEntity.builder()
                    .sede(sede)
                    .usuarioId(sesion.getUsuarioId())
                    .tipo(sesion.getTipo())
                    .estado(sesion.getEstado())
                    .saldoInicial(sesion.getSaldoInicial())
                    .totalIngresos(sesion.getTotalIngresos())
                    .totalEgresos(sesion.getTotalEgresos())
                    .saldoEsperado(sesion.getSaldoEsperado())
                    .saldoFinal(sesion.getSaldoFinal())
                    .diferencia(sesion.getDiferencia())
                    .fechaApertura(sesion.getFechaApertura())
                    .fechaCierre(sesion.getFechaCierre())
                    .cerradaPor(sesion.getCerradaPor())
                    .motivoCierre(sesion.getMotivoCierre())
                    .observaciones(sesion.getObservaciones())
                    .build();
        }
        return toDomain(jpaRepository.save(entity));
    }

    @Override
    @Transactional
    public void incrementarIngresos(Long id, BigDecimal delta) {
        jpaRepository.incrementarIngresos(id, delta);
    }

    @Override
    @Transactional
    public void incrementarEgresos(Long id, BigDecimal delta) {
        jpaRepository.incrementarEgresos(id, delta);
    }

    private SesionCaja toDomain(SesionCajaEntity e) {
        return SesionCaja.builder()
                .id(e.getId())
                .idSede(e.getSede().getId())
                .usuarioId(e.getUsuarioId())
                .tipo(e.getTipo())
                .estado(e.getEstado())
                .saldoInicial(e.getSaldoInicial())
                .totalIngresos(e.getTotalIngresos())
                .totalEgresos(e.getTotalEgresos())
                .saldoEsperado(e.getSaldoEsperado())
                .saldoFinal(e.getSaldoFinal())
                .diferencia(e.getDiferencia())
                .fechaApertura(e.getFechaApertura())
                .fechaCierre(e.getFechaCierre())
                .cerradaPor(e.getCerradaPor())
                .motivoCierre(e.getMotivoCierre())
                .observaciones(e.getObservaciones())
                .fechaCreacion(e.getFechaCreacion())
                .build();
    }
}
