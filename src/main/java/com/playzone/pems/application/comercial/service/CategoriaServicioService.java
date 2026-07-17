package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.ActualizarCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.query.CategoriaServicioQuery;
import com.playzone.pems.application.comercial.port.in.GestionarCategoriasServicioUseCase;
import com.playzone.pems.domain.comercial.model.CategoriaServicio;
import com.playzone.pems.domain.comercial.repository.CategoriaServicioRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.BusinessException;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoriaServicioService implements GestionarCategoriasServicioUseCase {

    private final CategoriaServicioRepository repository;
    private final SupabaseAuthFacade          authFacade;
    private final RegistrarLogUseCase         auditoria;

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaServicioQuery> listarActivas() {
        return repository.findAllActivas().stream().map(this::toQuery).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoriaServicioQuery> listarTodas() {
        return repository.findAll().stream().map(this::toQuery).toList();
    }

    @Override
    public CategoriaServicioQuery crear(CrearCategoriaServicioCommand command) {
        validarNombreUnico(command.getNombre(), null);
        CategoriaServicio categoria = CategoriaServicio.builder()
                .nombre(command.getNombre())
                .activo(command.isActivo())
                .orden(command.getOrden())
                .build();
        CategoriaServicioQuery resultado = toQuery(repository.save(categoria));
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_COMERCIAL,
                "CategoriaServicio", resultado.getId(),
                null, resultado.getNombre(),
                "Categoría de servicio creada: " + resultado.getNombre(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public CategoriaServicioQuery actualizar(ActualizarCategoriaServicioCommand command) {
        CategoriaServicio existente = repository.findById(command.getId())
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaServicio", command.getId()));
        validarNombreUnico(command.getNombre(), command.getId());
        CategoriaServicio actualizada = existente.toBuilder()
                .nombre(command.getNombre())
                .activo(command.isActivo())
                .orden(command.getOrden())
                .build();
        CategoriaServicioQuery resultado = toQuery(repository.save(actualizada));
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_COMERCIAL,
                "CategoriaServicio", command.getId(),
                existente.getNombre(), resultado.getNombre(),
                "Categoría de servicio actualizada: " + resultado.getNombre(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public void eliminar(Long id) {
        CategoriaServicio existente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CategoriaServicio", id));
        if (repository.tieneServiciosAsociados(id)) {
            throw new BusinessException(
                    "No se puede eliminar: existen servicios asociados a esta categoría.", HttpStatus.CONFLICT);
        }
        repository.deleteById(id);
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ELIMINAR, AuditoriaConstants.MOD_COMERCIAL,
                "CategoriaServicio", id,
                existente.getNombre(), null,
                "Categoría de servicio eliminada: " + existente.getNombre(),
                null, null, AuditoriaConstants.NIVEL_CRITICAL, AuditoriaConstants.RESULTADO_EXITOSO));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean duplicado = idExcluido == null
                ? repository.existsByNombre(nombre)
                : repository.existsByNombreExcludingId(nombre, idExcluido);
        if (duplicado) {
            throw new BusinessException("Ya existe una categoría con el nombre '" + nombre + "'.", HttpStatus.BAD_REQUEST);
        }
    }

    private CategoriaServicioQuery toQuery(CategoriaServicio c) {
        return CategoriaServicioQuery.builder()
                .id(c.getId())
                .nombre(c.getNombre())
                .orden(c.getOrden())
                .activo(c.isActivo())
                .fechaCreacion(c.getCreatedAt())
                .fechaActualizacion(c.getUpdatedAt())
                .build();
    }
}
