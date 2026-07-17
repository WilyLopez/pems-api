package com.playzone.pems.application.comercial.port.in;

import com.playzone.pems.application.comercial.dto.command.ActualizarServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioVarianteQuery;

import java.util.List;

public interface GestionarServicioVariantesUseCase {
    List<ServicioVarianteQuery> listarPorServicio(Long idServicio);
    ServicioVarianteQuery crear(Long idServicio, CrearServicioVarianteCommand command);
    ServicioVarianteQuery actualizar(Long idServicio, ActualizarServicioVarianteCommand command);
    void eliminar(Long idServicio, Long id);
    ServicioVarianteQuery reordenar(Long idServicio, Long id, int nuevoOrden);
}
