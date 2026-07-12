package com.playzone.pems.application.contrato.port.in;

import com.playzone.pems.application.contrato.dto.command.CargarContratoCommand;
import com.playzone.pems.application.contrato.dto.query.ContratoQuery;

public interface CargarContratoUseCase {
    ContratoQuery ejecutar(CargarContratoCommand command);
}
