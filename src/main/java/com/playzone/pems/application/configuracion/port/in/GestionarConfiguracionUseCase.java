package com.playzone.pems.application.configuracion.port.in;

import com.playzone.pems.domain.configuracion.model.ConfiguracionGlobal;

import java.util.List;
import java.util.Map;

public interface GestionarConfiguracionUseCase {

    List<ConfiguracionGlobal> listar();

    List<ConfiguracionGlobal> actualizar(Map<String, String> cambios);

    Map<String, String> obtenerPublicas();
}
