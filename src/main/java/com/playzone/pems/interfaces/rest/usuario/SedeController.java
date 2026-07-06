package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.application.usuario.port.in.GestionarSedeUseCase;
import com.playzone.pems.domain.usuario.model.Sede;
import com.playzone.pems.interfaces.rest.usuario.response.SedeResponse;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
public class SedeController {

    private final GestionarSedeUseCase gestionarSedeUseCase;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SedeResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(
            gestionarSedeUseCase.listar().stream().map(this::toResponse).toList()
        ));
    }

    @GetMapping("/{idSede}")
    public ResponseEntity<ApiResponse<SedeResponse>> obtener(@PathVariable Long idSede) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(gestionarSedeUseCase.obtener(idSede))));
    }

    @PutMapping("/{idSede}")
    @PreAuthorize("hasAuthority('configuracion.editar')")
    public ResponseEntity<ApiResponse<SedeResponse>> actualizar(
            @PathVariable Long idSede,
            @Valid @RequestBody ActualizarSedeRequest request) {

        Sede sede = gestionarSedeUseCase.actualizar(idSede,
                new GestionarSedeUseCase.ActualizarSedeCommand(
                        request.getNombre(),
                        request.getCiudad(),
                        request.getDepartamento(),
                        request.getRuc(),
                        request.getLatitud(),
                        request.getLongitud(),
                        request.getGoogleMapsEmbedUrl()));

        return ResponseEntity.ok(ApiResponse.ok(toResponse(sede)));
    }

    private SedeResponse toResponse(Sede s) {
        return SedeResponse.builder()
                .id(s.getId())
                .nombre(s.getNombre())
                .ciudad(s.getCiudad())
                .departamento(s.getDepartamento())
                .ruc(s.getRuc())
                .latitud(s.getLatitud())
                .longitud(s.getLongitud())
                .googleMapsEmbedUrl(s.getGoogleMapsEmbedUrl())
                .activo(s.getDeletedAt() == null)
                .fechaCreacion(s.getFechaCreacion())
                .build();
    }

    @Getter @NoArgsConstructor
    public static class ActualizarSedeRequest {
        @NotBlank @Size(max = 120) private String nombre;
        @NotBlank @Size(max = 80)  private String ciudad;
        @NotBlank @Size(max = 80)  private String departamento;
        @Size(max = 11)            private String  ruc;
        private Double latitud;
        private Double longitud;
        private String googleMapsEmbedUrl;
    }
}
