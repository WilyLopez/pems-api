package com.playzone.pems.application.finanzas.port.in;

import com.playzone.pems.application.finanzas.dto.command.AnularEgresoCommand;
import com.playzone.pems.application.finanzas.dto.command.AprobarEgresoCommand;
import com.playzone.pems.application.finanzas.dto.command.RechazarEgresoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarEgresoCommand;
import com.playzone.pems.application.finanzas.dto.query.RegistroEgresoQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RegistrarEgresoUseCase {
    RegistroEgresoQuery registrar(RegistrarEgresoCommand command);
    Page<RegistroEgresoQuery> listar(Long idSede, Pageable pageable);
    List<RegistroEgresoQuery> listarPorPeriodo(Long idSede, int anio, int mes);
    List<RegistroEgresoQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin);
    List<RegistroEgresoQuery> listarPendientesAprobacion(Long idSede);
    RegistroEgresoQuery anular(AnularEgresoCommand command);
    RegistroEgresoQuery aprobar(AprobarEgresoCommand command);
    RegistroEgresoQuery rechazar(RechazarEgresoCommand command);
}
