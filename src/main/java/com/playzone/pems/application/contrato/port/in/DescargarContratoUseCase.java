package com.playzone.pems.application.contrato.port.in;

import com.playzone.pems.application.contrato.dto.query.ArchivoContratoQuery;

public interface DescargarContratoUseCase {
    ArchivoContratoQuery ejecutar(Long idEventoPrivado);
}
