package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.SesionCaja;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SesionCajaRepository {
    Optional<SesionCaja> findById(Long id);
    Optional<SesionCaja> findByIdForUpdate(Long id);
    Optional<SesionCaja> findAbiertaByUsuario(UUID usuarioId);
    Optional<SesionCaja> findAbiertaByUsuarioAndSede(UUID usuarioId, Long idSede);
    Optional<SesionCaja> findAbiertaBySedeAndTipo(Long idSede, TipoSesionCaja tipo);
    Optional<SesionCaja> findByUsuarioAndSedeAndFecha(UUID usuarioId, Long idSede, LocalDate fecha);
    boolean existsAbiertaBySede(Long idSede);
    List<SesionCaja> findAllAbiertas();
    List<SesionCaja> findBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);
    SesionCaja save(SesionCaja sesion);
    int incrementarIngresosSiAbierta(Long id, BigDecimal delta);
    int incrementarEgresosSiAbierta(Long id, BigDecimal delta);
}
