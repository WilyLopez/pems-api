package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.RegistroEgreso;
import com.playzone.pems.domain.finanzas.model.enums.CategoriaEgreso;
import com.playzone.pems.domain.finanzas.query.MontoPorDia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegistroEgresoRepository {
    Optional<RegistroEgreso> findById(Long id);
    Optional<RegistroEgreso> findByIdForUpdate(Long id);
    boolean existsAnuladoPara(Long idRegistroOriginal);
    List<RegistroEgreso> findPendientesAprobacion(Long idSede);
    Page<RegistroEgreso> findBySede(Long idSede, Pageable pageable);
    List<RegistroEgreso> findBySedeAndPeriodo(Long idSede, int anio, int mes);
    List<RegistroEgreso> findBySedeAndRangoFecha(Long idSede, LocalDate inicio, LocalDate fin);
    BigDecimal sumMontoBySedeAndPeriodo(Long idSede, int anio, int mes);
    BigDecimal sumMontoBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);
    Map<String, BigDecimal> sumMontoAgrupadoPorTipo(Long idSede, int anio, int mes);
    Map<CategoriaEgreso, BigDecimal> sumMontoAgrupadoPorCategoria(Long idSede, int anio, int mes);
    List<MontoPorDia> sumMontoAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin);
    RegistroEgreso save(RegistroEgreso registro);
    void deleteById(Long id);
}
