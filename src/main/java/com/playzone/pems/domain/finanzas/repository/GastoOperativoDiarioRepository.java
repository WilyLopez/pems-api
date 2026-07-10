package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.GastoOperativoDiario;
import com.playzone.pems.domain.finanzas.query.GastosPorDia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface GastoOperativoDiarioRepository {
    Optional<GastoOperativoDiario> findById(Long id);
    boolean existsAnuladoPara(Long idGastoOriginal);
    List<GastoOperativoDiario> findBySedeAndFecha(Long idSede, LocalDate fecha);
    List<GastoOperativoDiario> findBySedeAndRangoFecha(Long idSede, LocalDate inicio, LocalDate fin);
    BigDecimal sumMontoBySedeAndFecha(Long idSede, LocalDate fecha);
    BigDecimal sumMontoBySedeAndPeriodo(Long idSede, int anio, int mes);
    BigDecimal sumMontoBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);
    List<GastosPorDia> sumMontoAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin);
    GastoOperativoDiario save(GastoOperativoDiario gasto);
}
