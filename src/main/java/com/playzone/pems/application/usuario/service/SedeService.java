package com.playzone.pems.application.usuario.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.usuario.port.in.GestionarSedeUseCase;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeService implements GestionarSedeUseCase {

    private final SedeRepository   sedeRepository;
    private final SupabaseAuthFacade authFacade;
    private final RegistrarLogUseCase auditoria;

    @Override
    public List<Sede> listar() {
        return sedeRepository.findAllActivas();
    }

    @Override
    public Sede obtener(Long idSede) {
        return sedeRepository.findById(idSede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", idSede));
    }

    @Override
    @Transactional
    public Sede actualizar(Long idSede, ActualizarSedeCommand command) {
        Sede sede = sedeRepository.findById(idSede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede", idSede));

        Sede actualizada = sedeRepository.save(sede.toBuilder()
                .nombre(command.nombre() != null ? command.nombre() : sede.getNombre())
                .ciudad(command.ciudad() != null ? command.ciudad() : sede.getCiudad())
                .departamento(command.departamento() != null ? command.departamento() : sede.getDepartamento())
                .ruc(command.ruc())
                .latitud(command.latitud())
                .longitud(command.longitud())
                .googleMapsEmbedUrl(command.googleMapsEmbedUrl())
                .build());

        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_CONFIGURACION,
                "Sede", idSede,
                sede.getNombre(), actualizada.getNombre(),
                "Sede actualizada: " + actualizada.getNombre(),
                null, null, AuditoriaConstants.NIVEL_WARNING, AuditoriaConstants.RESULTADO_EXITOSO));

        return actualizada;
    }
}
