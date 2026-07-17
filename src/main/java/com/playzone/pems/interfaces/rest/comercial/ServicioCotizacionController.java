package com.playzone.pems.interfaces.rest.comercial;

import com.playzone.pems.application.comercial.dto.command.ActualizarServicioCommand;
import com.playzone.pems.application.comercial.dto.command.ActualizarServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearServicioVarianteCommand;
import com.playzone.pems.application.comercial.dto.command.SubirServicioImagenCommand;
import com.playzone.pems.application.comercial.dto.query.ServicioCotizacionQuery;
import com.playzone.pems.application.comercial.dto.query.ServicioImagenQuery;
import com.playzone.pems.application.comercial.dto.query.ServicioVarianteQuery;
import com.playzone.pems.application.comercial.port.in.GestionarServiciosCotizacionUseCase;
import com.playzone.pems.application.comercial.port.in.GestionarServicioImagenesUseCase;
import com.playzone.pems.application.comercial.port.in.GestionarServicioVariantesUseCase;
import com.playzone.pems.interfaces.rest.comercial.response.ServicioCotizacionResponse;
import com.playzone.pems.interfaces.rest.comercial.response.ServicioImagenResponse;
import com.playzone.pems.interfaces.rest.comercial.response.ServicioVarianteResponse;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/servicios-cotizacion")
@RequiredArgsConstructor
public class ServicioCotizacionController {

    private final GestionarServiciosCotizacionUseCase useCase;
    private final GestionarServicioVariantesUseCase   variantesUseCase;
    private final GestionarServicioImagenesUseCase    imagenesUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServicioCotizacionResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.listarActivos().stream().map(this::toResponse).toList()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('servicio.ver')")
    public ResponseEntity<ApiResponse<List<ServicioCotizacionResponse>>> listarAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.listarTodos().stream().map(this::toResponse).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioCotizacionResponse>> crear(@Valid @RequestBody CrearServicioCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(useCase.crear(command))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioCotizacionResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarServicioCommand command) {
        ActualizarServicioCommand withId = command.toBuilder().id(id).build();
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.actualizar(withId))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping("/{idServicio}/variantes")
    public ResponseEntity<ApiResponse<List<ServicioVarianteResponse>>> listarVariantes(@PathVariable Long idServicio) {
        return ResponseEntity.ok(ApiResponse.ok(
                variantesUseCase.listarPorServicio(idServicio).stream().map(this::toVarianteResponse).toList()));
    }

    @PostMapping("/{idServicio}/variantes")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioVarianteResponse>> crearVariante(
            @PathVariable Long idServicio,
            @Valid @RequestBody CrearServicioVarianteCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                toVarianteResponse(variantesUseCase.crear(idServicio, command))));
    }

    @PutMapping("/{idServicio}/variantes/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioVarianteResponse>> actualizarVariante(
            @PathVariable Long idServicio,
            @PathVariable Long id,
            @Valid @RequestBody ActualizarServicioVarianteCommand command) {
        ActualizarServicioVarianteCommand withId = command.toBuilder().id(id).build();
        return ResponseEntity.ok(ApiResponse.ok(
                toVarianteResponse(variantesUseCase.actualizar(idServicio, withId))));
    }

    @DeleteMapping("/{idServicio}/variantes/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<Void>> eliminarVariante(
            @PathVariable Long idServicio,
            @PathVariable Long id) {
        variantesUseCase.eliminar(idServicio, id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/{idServicio}/variantes/{id}/orden")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioVarianteResponse>> reordenarVariante(
            @PathVariable Long idServicio,
            @PathVariable Long id,
            @RequestParam int nuevoOrden) {
        return ResponseEntity.ok(ApiResponse.ok(
                toVarianteResponse(variantesUseCase.reordenar(idServicio, id, nuevoOrden))));
    }

    @GetMapping("/{idServicio}/imagenes")
    public ResponseEntity<ApiResponse<List<ServicioImagenResponse>>> listarImagenes(@PathVariable Long idServicio) {
        return ResponseEntity.ok(ApiResponse.ok(
                imagenesUseCase.listarPorServicio(idServicio).stream().map(this::toImagenResponse).toList()));
    }

    @PostMapping("/{idServicio}/imagenes")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioImagenResponse>> subirImagen(
            @PathVariable Long idServicio,
            @RequestParam MultipartFile archivo,
            @RequestParam(required = false) Long idVariante,
            @RequestParam(required = false) String altTexto,
            @RequestParam(defaultValue = "0") int orden) throws IOException {
        SubirServicioImagenCommand command = SubirServicioImagenCommand.builder()
                .contenido(archivo.getBytes())
                .nombreArchivo(archivo.getOriginalFilename())
                .contentType(archivo.getContentType())
                .idVariante(idVariante)
                .altTexto(altTexto)
                .orden(orden)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                toImagenResponse(imagenesUseCase.subir(idServicio, command))));
    }

    @DeleteMapping("/{idServicio}/imagenes/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<Void>> eliminarImagen(
            @PathVariable Long idServicio,
            @PathVariable Long id) {
        imagenesUseCase.eliminar(idServicio, id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/{idServicio}/imagenes/{id}/orden")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioImagenResponse>> reordenarImagen(
            @PathVariable Long idServicio,
            @PathVariable Long id,
            @RequestParam int nuevoOrden) {
        return ResponseEntity.ok(ApiResponse.ok(
                toImagenResponse(imagenesUseCase.reordenar(idServicio, id, nuevoOrden))));
    }

    @PatchMapping("/{idServicio}/imagenes/{id}/principal")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<ServicioImagenResponse>> marcarPrincipal(
            @PathVariable Long idServicio,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                toImagenResponse(imagenesUseCase.marcarPrincipal(idServicio, id))));
    }

    private ServicioCotizacionResponse toResponse(ServicioCotizacionQuery q) {
        return ServicioCotizacionResponse.builder()
                .id(q.getId())
                .nombre(q.getNombre())
                .descripcion(q.getDescripcion())
                .precioReferencial(q.getPrecioReferencial())
                .icono(q.getIcono())
                .activo(q.isActivo())
                .destacado(q.isDestacado())
                .categoriaId(q.getCategoriaId())
                .categoriaNombre(q.getCategoriaNombre())
                .orden(q.getOrden())
                .fechaCreacion(q.getFechaCreacion())
                .fechaActualizacion(q.getFechaActualizacion())
                .tieneVariantes(q.isTieneVariantes())
                .precioDesde(q.getPrecioDesde())
                .variantes(q.getVariantes() != null
                        ? q.getVariantes().stream().map(this::toVarianteResponse).toList()
                        : List.of())
                .imagenPrincipal(q.getImagenPrincipal())
                .imagenes(q.getImagenes() != null
                        ? q.getImagenes().stream().map(this::toImagenResponse).toList()
                        : List.of())
                .build();
    }

    private ServicioVarianteResponse toVarianteResponse(ServicioVarianteQuery v) {
        return ServicioVarianteResponse.builder()
                .id(v.getId())
                .idServicio(v.getIdServicio())
                .nombre(v.getNombre())
                .descripcion(v.getDescripcion())
                .precio(v.getPrecio())
                .activo(v.isActivo())
                .orden(v.getOrden())
                .build();
    }

    private ServicioImagenResponse toImagenResponse(ServicioImagenQuery i) {
        return ServicioImagenResponse.builder()
                .id(i.getId())
                .idServicio(i.getIdServicio())
                .idVariante(i.getIdVariante())
                .url(i.getUrl())
                .altTexto(i.getAltTexto())
                .orden(i.getOrden())
                .esPrincipal(i.isEsPrincipal())
                .build();
    }
}
