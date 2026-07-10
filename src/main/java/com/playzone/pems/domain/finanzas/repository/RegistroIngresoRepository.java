package com.playzone.pems.domain.finanzas.repository;

import com.playzone.pems.domain.finanzas.model.RegistroIngreso;
import com.playzone.pems.domain.finanzas.query.MontoPorDia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface RegistroIngresoRepository {
    Optional<RegistroIngreso> findById(Long id);
    boolean existsAnuladoPara(Long idRegistroOriginal);
    List<RegistroIngreso> findTesoreriaWeb(Long idSede, LocalDate inicio, LocalDate fin);
    Page<RegistroIngreso> findBySede(Long idSede, Pageable pageable);
    List<RegistroIngreso> findBySedeAndRangoFecha(Long idSede, LocalDate inicio, LocalDate fin);
    List<RegistroIngreso> findBySedeAndPeriodo(Long idSede, int anio, int mes);
    BigDecimal sumMontoBySedeAndPeriodo(Long idSede, int anio, int mes);
    BigDecimal sumMontoBySedeAndRango(Long idSede, LocalDate inicio, LocalDate fin);
    BigDecimal sumMontoBySedeAndRangoCobro(Long idSede, LocalDate inicio, LocalDate fin);
    Map<String, BigDecimal> sumMontoAgrupadoPorTipo(Long idSede, int anio, int mes);
    List<MontoPorDia> sumMontoAgrupadoPorDia(Long idSede, LocalDate inicio, LocalDate fin);
    RegistroIngreso save(RegistroIngreso registro);
    void deleteById(Long id);
}
