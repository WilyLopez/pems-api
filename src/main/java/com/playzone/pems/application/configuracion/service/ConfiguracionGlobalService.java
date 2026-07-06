package com.playzone.pems.application.configuracion.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.configuracion.port.in.GestionarConfiguracionUseCase;
import com.playzone.pems.domain.configuracion.model.ConfiguracionGlobal;
import com.playzone.pems.domain.configuracion.repository.ConfiguracionGlobalRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfiguracionGlobalService implements GestionarConfiguracionUseCase {

    private static final Set<String> CLAVES_PUBLICAS = Set.of(
            "EDAD_MIN_NINO", "EDAD_MAX_NINO");

    private final ConfiguracionGlobalRepository configuracionRepository;
    private final SupabaseAuthFacade            authFacade;
    private final RegistrarLogUseCase           auditoria;

    @Override
    public List<ConfiguracionGlobal> listar() {
        return configuracionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> obtenerPublicas() {
        return configuracionRepository.findAll().stream()
                .filter(c -> CLAVES_PUBLICAS.contains(c.getClave()))
                .collect(Collectors.toMap(
                        ConfiguracionGlobal::getClave, ConfiguracionGlobal::getValor));
    }

    @Override
    @Transactional
    public List<ConfiguracionGlobal> actualizar(Map<String, String> cambios) {
        List<ConfiguracionGlobal> pendientes = new ArrayList<>();
        for (Map.Entry<String, String> entry : cambios.entrySet()) {
            ConfiguracionGlobal config = configuracionRepository.findByClave(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("Configuracion", "clave", entry.getKey()));
            pendientes.add(config.toBuilder().valor(entry.getValue()).build());
        }
        List<ConfiguracionGlobal> resultado = configuracionRepository.saveAll(pendientes);

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_CONFIGURACION,
                "ConfiguracionGlobal", null,
                null, cambios.keySet().toString(),
                "Configuración global actualizada: " + cambios.size() + " clave(s)",
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        return resultado;
    }
}
