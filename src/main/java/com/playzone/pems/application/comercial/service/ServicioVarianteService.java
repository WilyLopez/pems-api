package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.ActualizarServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioVarianteQuery;
import com.playzone.pems.application.comercial.port.in.GestionarServicioVariantesUseCase;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
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
public class ServicioVarianteService implements GestionarServicioVariantesUseCase {

    private final ServicioVarianteRepository   repository;
    private final ServicioCotizacionRepository servicioRepository;
    private final SupabaseAuthFacade           authFacade;
    private final RegistrarLogUseCase          auditoria;

    @Override
    @Transactional(readOnly = true)
    public List<ServicioVarianteQuery> listarPorServicio(Long idServicio) {
        return repository.findByServicio(idServicio).stream().map(this::toQuery).toList();
    }

    @Override
    public ServicioVarianteQuery crear(Long idServicio, CrearServicioVarianteCommand command) {
        servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("ServicioCotizacion", idServicio));
        validarNombreUnico(idServicio, command.getNombre(), null);
        ServicioVariante variante = ServicioVariante.builder()
                .idServicio(idServicio)
                .nombre(command.getNombre())
                .descripcion(command.getDescripcion())
                .precio(command.getPrecio())
                .activo(command.isActivo())
                .orden(command.getOrden())
                .build();
        ServicioVarianteQuery resultado = toQuery(repository.save(variante));
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_COMERCIAL,
                "ServicioVariante", resultado.getId(),
                null, resultado.getNombre(),
                "Variante creada: " + resultado.getNombre(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public ServicioVarianteQuery actualizar(Long idServicio, ActualizarServicioVarianteCommand command) {
        ServicioVariante existente = obtenerDeServicio(idServicio, command.getId());
        validarNombreUnico(idServicio, command.getNombre(), command.getId());
        ServicioVariante actualizado = existente.toBuilder()
                .nombre(command.getNombre())
                .descripcion(command.getDescripcion())
                .precio(command.getPrecio())
                .activo(command.isActivo())
                .orden(command.getOrden())
                .build();
        ServicioVarianteQuery resultado = toQuery(repository.save(actualizado));
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_COMERCIAL,
                "ServicioVariante", command.getId(),
                existente.getNombre(), resultado.getNombre(),
                "Variante actualizada: " + resultado.getNombre(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public void eliminar(Long idServicio, Long id) {
        ServicioVariante existente = obtenerDeServicio(idServicio, id);
        repository.deleteById(id);
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ELIMINAR, AuditoriaConstants.MOD_COMERCIAL,
                "ServicioVariante", id,
                existente.getNombre(), null,
                "Variante eliminada: " + existente.getNombre(),
                null, null, AuditoriaConstants.NIVEL_CRITICAL, AuditoriaConstants.RESULTADO_EXITOSO));
    }

    @Override
    public ServicioVarianteQuery reordenar(Long idServicio, Long id, int nuevoOrden) {
        ServicioVariante variante = obtenerDeServicio(idServicio, id);
        List<ServicioVariante> todas = repository.findByServicio(idServicio);
        todas.stream()
                .filter(v -> v.getOrden() == nuevoOrden && !v.getId().equals(id))
                .findFirst()
                .ifPresent(otra -> repository.save(otra.toBuilder().orden(variante.getOrden()).build()));
        return toQuery(repository.save(variante.toBuilder().orden(nuevoOrden).build()));
    }

    private ServicioVariante obtenerDeServicio(Long idServicio, Long id) {
        ServicioVariante variante = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServicioVariante", id));
        if (!variante.getIdServicio().equals(idServicio)) {
            throw new ResourceNotFoundException("ServicioVariante", id);
        }
        return variante;
    }

    private void validarNombreUnico(Long idServicio, String nombre, Long idExcluido) {
        boolean duplicado = idExcluido == null
                ? repository.existsByServicioAndNombre(idServicio, nombre)
                : repository.existsByServicioAndNombreExcludingId(idServicio, nombre, idExcluido);
        if (duplicado) {
            throw new BusinessException("Ya existe una variante con el nombre '" + nombre + "' en este servicio.", HttpStatus.BAD_REQUEST);
        }
    }

    private ServicioVarianteQuery toQuery(ServicioVariante v) {
        return ServicioVarianteQuery.builder()
                .id(v.getId())
                .idServicio(v.getIdServicio())
                .nombre(v.getNombre())
                .descripcion(v.getDescripcion())
                .precio(v.getPrecio())
                .activo(v.isActivo())
                .orden(v.getOrden())
                .build();
    }
}
