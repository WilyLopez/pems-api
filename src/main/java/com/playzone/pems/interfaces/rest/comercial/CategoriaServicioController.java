package com.playzone.pems.interfaces.rest.comercial;

import com.playzone.pems.application.comercial.dto.command.ActualizarCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.command.CrearCategoriaServicioCommand;
import com.playzone.pems.application.comercial.dto.query.CategoriaServicioQuery;
import com.playzone.pems.application.comercial.port.in.GestionarCategoriasServicioUseCase;
import com.playzone.pems.interfaces.rest.comercial.response.CategoriaServicioResponse;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias-servicio")
@RequiredArgsConstructor
public class CategoriaServicioController {

    private final GestionarCategoriasServicioUseCase useCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaServicioResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.listarActivas().stream().map(this::toResponse).toList()));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('servicio.ver')")
    public ResponseEntity<ApiResponse<List<CategoriaServicioResponse>>> listarAdmin() {
        return ResponseEntity.ok(ApiResponse.ok(useCase.listarTodas().stream().map(this::toResponse).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<CategoriaServicioResponse>> crear(@Valid @RequestBody CrearCategoriaServicioCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(useCase.crear(command))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<CategoriaServicioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCategoriaServicioCommand command) {
        ActualizarCategoriaServicioCommand withId = command.toBuilder().id(id).build();
        return ResponseEntity.ok(ApiResponse.ok(toResponse(useCase.actualizar(withId))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('servicio.gestionar')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable Long id) {
        useCase.eliminar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    private CategoriaServicioResponse toResponse(CategoriaServicioQuery q) {
        return CategoriaServicioResponse.builder()
                .id(q.getId())
                .nombre(q.getNombre())
                .orden(q.getOrden())
                .activo(q.isActivo())
                .fechaCreacion(q.getFechaCreacion())
                .fechaActualizacion(q.getFechaActualizacion())
                .build();
    }
}
