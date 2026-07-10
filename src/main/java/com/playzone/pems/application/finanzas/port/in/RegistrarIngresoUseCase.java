package com.playzone.pems.application.finanzas.port.in;

import com.playzone.pems.application.finanzas.dto.command.AnularIngresoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarIngresoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.RegistroIngresoQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RegistrarIngresoUseCase {
    RegistroIngresoQuery registrar(RegistrarIngresoManualCommand command);
    Page<RegistroIngresoQuery> listar(Long idSede, Pageable pageable);
    List<RegistroIngresoQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin);
    List<RegistroIngresoQuery> listarTesoreriaWeb(Long idSede, LocalDate inicio, LocalDate fin);
    RegistroIngresoQuery anular(AnularIngresoCommand command);
}
