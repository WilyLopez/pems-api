package com.playzone.pems.application.finanzas.port.in;

import com.playzone.pems.application.finanzas.dto.command.AnularGastoOperativoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarGastoOperativoCommand;
import com.playzone.pems.application.finanzas.dto.query.GastoOperativoQuery;

import java.time.LocalDate;
import java.util.List;

public interface GestionarGastoOperativoUseCase {
    GastoOperativoQuery registrar(RegistrarGastoOperativoCommand command);
    GastoOperativoQuery anular(AnularGastoOperativoCommand command);
    List<GastoOperativoQuery> listarPorFecha(Long idSede, LocalDate fecha);
    List<GastoOperativoQuery> listarPorRango(Long idSede, LocalDate inicio, LocalDate fin);
}
