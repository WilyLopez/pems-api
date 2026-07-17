package com.playzone.pems.application.comercial.port.in;

import com.playzone.pems.application.comercial.dto.command.ActualizarCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.query.CategoriaServicioQuery;

import java.util.List;

public interface GestionarCategoriasServicioUseCase {
    List<CategoriaServicioQuery> listarActivas();
    List<CategoriaServicioQuery> listarTodas();
    CategoriaServicioQuery crear(CrearCategoriaServicioCommand command);
    CategoriaServicioQuery actualizar(ActualizarCategoriaServicioCommand command);
    void eliminar(Long id);
}
