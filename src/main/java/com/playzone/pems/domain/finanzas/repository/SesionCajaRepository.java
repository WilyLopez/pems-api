package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.SesionCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionCajaRepository {
    Optional<SesionCaja> findById(Long id);
    Optional<SesionCaja> findAbiertaByUsuario(UUID usuarioId);
    Optional<SesionCaja> findAbiertaByUsuarioAndSede(UUID usuarioId, Long idSede);
    Optional<SesionCaja> findByUsuarioAndSedeAndFecha(UUID usuarioId, Long idSede, LocalDate fecha);
    boolean existsAbiertaBySede(Long idSede);
    List<SesionCaja> findBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);
    SesionCaja save(SesionCaja sesion);
    void incrementarIngresos(Long id, BigDecimal delta);
    void incrementarEgresos(Long id, BigDecimal delta);
}
