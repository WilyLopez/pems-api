package com.playzone.pems.application.comercial.service;

import com.playzone.pems.application.auditoria.AuditoriaConstants;
import com.playzone.pems.application.auditoria.port.in.RegistrarLogUseCase;
import com.playzone.pems.application.comercial.dto.command.ActualizarServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioCotizacionQuery;
import com.playzone.pems.application.comercial.dto.query.ServicioImagenQuery;
import com.playzone.pems.application.comercial.dto.query.ServicioVarianteQuery;
import com.playzone.pems.application.comercial.port.in.GestionarServiciosCotizacionUseCase;
import com.playzone.pems.domain.comercial.model.CategoriaServicio;
import com.playzone.pems.domain.comercial.model.ServicioCotizacion;
import com.playzone.pems.domain.comercial.model.ServicioImagen;
import com.playzone.pems.domain.comercial.model.ServicioVariante;
import com.playzone.pems.domain.comercial.repository.CategoriaServicioRepository;
import com.playzone.pems.domain.comercial.repository.ServicioCotizacionRepository;
import com.playzone.pems.domain.comercial.repository.ServicioImagenRepository;
import com.playzone.pems.domain.comercial.repository.ServicioVarianteRepository;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.BusinessException;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ServicioCotizacionService implements GestionarServiciosCotizacionUseCase {

    private final ServicioCotizacionRepository repository;
    private final ServicioVarianteRepository   varianteRepository;
    private final ServicioImagenRepository     imagenRepository;
    private final CategoriaServicioRepository  categoriaRepository;
    private final SupabaseAuthFacade           authFacade;
    private final RegistrarLogUseCase          auditoria;

    @Override
    @Transactional(readOnly = true)
    public List<ServicioCotizacionQuery> listarActivos() {
        return enriquecer(repository.findAllActivos());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServicioCotizacionQuery> listarTodos() {
        return enriquecer(repository.findAll());
    }

    @Override
    public ServicioCotizacionQuery crear(CrearServicioCommand command) {
        validarNombreUnico(command.getNombre(), null);
        CategoriaServicio categoria = validarCategoria(command.getCategoriaId());
        ServicioCotizacion servicio = ServicioCotizacion.builder()
                .nombre(command.getNombre())
                .descripcion(command.getDescripcion())
                .precioReferencial(command.getPrecioReferencial())
                .icono(command.getIcono())
                .activo(command.isActivo())
                .destacado(command.isDestacado())
                .categoriaId(command.getCategoriaId())
                .orden(command.getOrden())
                .build();
        ServicioCotizacionQuery resultado = toQuery(repository.save(servicio), Collections.emptyList(), Collections.emptyList(),
                categoria != null ? categoria.getNombre() : null);
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_CREAR, AuditoriaConstants.MOD_COMERCIAL,
                "ServicioCotizacion", resultado.getId(),
                null, resultado.getNombre(),
                "Servicio creado: " + resultado.getNombre(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public ServicioCotizacionQuery actualizar(ActualizarServicioCommand command) {
        ServicioCotizacion existente = repository.findById(command.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ServicioCotizacion", command.getId()));
        validarNombreUnico(command.getNombre(), command.getId());
        CategoriaServicio categoria = validarCategoria(command.getCategoriaId());
        ServicioCotizacion actualizado = existente.toBuilder()
                .nombre(command.getNombre())
                .descripcion(command.getDescripcion())
                .precioReferencial(command.getPrecioReferencial())
                .icono(command.getIcono())
                .activo(command.isActivo())
                .destacado(command.isDestacado())
                .categoriaId(command.getCategoriaId())
                .orden(command.getOrden())
                .build();
        List<ServicioVariante> variantes = varianteRepository.findByServicio(command.getId());
        List<ServicioImagen> imagenes = imagenRepository.findByServicio(command.getId());
        ServicioCotizacionQuery resultado = toQuery(repository.save(actualizado), variantes, imagenes,
                categoria != null ? categoria.getNombre() : null);
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ACTUALIZAR, AuditoriaConstants.MOD_COMERCIAL,
                "ServicioCotizacion", command.getId(),
                existente.getNombre(), resultado.getNombre(),
                "Servicio actualizado: " + resultado.getNombre(),
                null, null, AuditoriaConstants.NIVEL_INFO, AuditoriaConstants.RESULTADO_EXITOSO));
        return resultado;
    }

    @Override
    public void eliminar(Long id) {
        ServicioCotizacion existente = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ServicioCotizacion", id));
        repository.deleteById(id);
        auditoria.ejecutar(new RegistrarLogUseCase.Command(
                authFacade.usuarioActualId().orElse(null),
                AuditoriaConstants.ACCION_ELIMINAR, AuditoriaConstants.MOD_COMERCIAL,
                "ServicioCotizacion", id,
                existente.getNombre(), null,
                "Servicio eliminado: " + existente.getNombre(),
                null, null, AuditoriaConstants.NIVEL_CRITICAL, AuditoriaConstants.RESULTADO_EXITOSO));
    }

    private void validarNombreUnico(String nombre, Long idExcluido) {
        boolean duplicado = idExcluido == null
                ? repository.existsByNombre(nombre)
                : repository.existsByNombreExcludingId(nombre, idExcluido);
        if (duplicado) {
            throw new BusinessException("Ya existe un servicio con el nombre '" + nombre + "'.", HttpStatus.BAD_REQUEST);
        }
    }

    private CategoriaServicio validarCategoria(Long categoriaId) {
        if (categoriaId == null) return null;
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new BusinessException("La categoría seleccionada no existe.", HttpStatus.BAD_REQUEST));
    }

    private List<ServicioCotizacionQuery> enriquecer(List<ServicioCotizacion> servicios) {
        List<Long> ids = servicios.stream().map(ServicioCotizacion::getId).toList();
        Map<Long, List<ServicioVariante>> variantesPorServicio = varianteRepository.findByServicios(ids);
        Map<Long, List<ServicioImagen>> imagenesPorServicio = imagenRepository.findByServicios(ids);
        Map<Long, String> nombresPorCategoria = categoriaRepository.findAll().stream()
                .collect(Collectors.toMap(CategoriaServicio::getId, CategoriaServicio::getNombre));
        return servicios.stream()
                .map(s -> toQuery(s,
                        variantesPorServicio.getOrDefault(s.getId(), Collections.emptyList()),
                        imagenesPorServicio.getOrDefault(s.getId(), Collections.emptyList()),
                        nombresPorCategoria.get(s.getCategoriaId())))
                .toList();
    }

    private ServicioCotizacionQuery toQuery(ServicioCotizacion s, List<ServicioVariante> variantes, List<ServicioImagen> imagenes,
                                             String categoriaNombre) {
        ServicioCotizacion enriquecido = s.toBuilder().variantes(variantes).imagenes(imagenes).build();
        return ServicioCotizacionQuery.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .descripcion(s.getDescripcion())
                .precioReferencial(s.getPrecioReferencial())
                .icono(s.getIcono())
                .activo(s.isActivo())
                .destacado(s.isDestacado())
                .categoriaId(s.getCategoriaId())
                .categoriaNombre(categoriaNombre)
                .orden(s.getOrden())
                .fechaCreacion(s.getCreatedAt())
                .fechaActualizacion(s.getUpdatedAt())
                .tieneVariantes(enriquecido.tieneVariantes())
                .precioDesde(enriquecido.precioDesde())
                .variantes(variantes.stream().map(this::toVarianteQuery).toList())
                .imagenPrincipal(enriquecido.imagenPrincipal())
                .imagenes(imagenes.stream().map(this::toImagenQuery).toList())
                .build();
    }

    private ServicioVarianteQuery toVarianteQuery(ServicioVariante v) {
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

    private ServicioImagenQuery toImagenQuery(ServicioImagen i) {
        return ServicioImagenQuery.builder()
                .id(i.getId())
                .idServicio(i.getIdServicio())
                .idVariante(i.getIdVariante())
                .url(i.getArchivoPath())
                .altTexto(i.getAltTexto())
                .orden(i.getOrden())
                .esPrincipal(i.isEsPrincipal())
                .build();
    }
}
