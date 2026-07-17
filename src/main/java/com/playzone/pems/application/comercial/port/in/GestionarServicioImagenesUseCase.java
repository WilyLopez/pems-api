package com.playzone.pems.application.comercial.port.in;

import com.playzone.pems.application.comercial.dto.command.SubirServicioImagenCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioImagenQuery;

import java.util.List;

public interface GestionarServicioImagenesUseCase {
    List<ServicioImagenQuery> listarPorServicio(Long idServicio);
    ServicioImagenQuery subir(Long idServicio, SubirServicioImagenCommand command);
    void eliminar(Long idServicio, Long id);
    ServicioImagenQuery reordenar(Long idServicio, Long id, int nuevoOrden);
    ServicioImagenQuery marcarPrincipal(Long idServicio, Long id);
}
