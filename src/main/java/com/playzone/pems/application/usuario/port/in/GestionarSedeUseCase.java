package com.playzone.pems.application.usuario.port.in;

import com.playzone.pems.domain.usuario.model.Sede;

import java.util.List;

public interface GestionarSedeUseCase {

    record ActualizarSedeCommand(
            String nombre,
            String ciudad,
            String departamento,
            String ruc,
            Double latitud,
            Double longitud,
            String googleMapsEmbedUrl
    ) {}

    List<Sede> listar();

    Sede obtener(Long idSede);

    Sede actualizar(Long idSede, ActualizarSedeCommand command);
}
