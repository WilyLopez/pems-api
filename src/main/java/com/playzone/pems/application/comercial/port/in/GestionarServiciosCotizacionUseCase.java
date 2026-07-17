package com.playzone.pems.application.comercial.port.in;

import com.playzone.pems.application.comercial.dto.command.ActualizarServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioCotizacionQuery;

import java.util.List;

public interface GestionarServiciosCotizacionUseCase {
    List<ServicioCotizacionQuery> listarActivos();
    List<ServicioCotizacionQuery> listarTodos();
    ServicioCotizacionQuery crear(CrearServicioCommand command);
    ServicioCotizacionQuery actualizar(ActualizarServicioCommand command);
    void eliminar(Long id);
}
