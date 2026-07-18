package com.playzone.pems.interfaces.rest.auditoria;

import com.playzone.pems.application.auditoria.port.in.ObtenerAuditoriaUseCase;
import com.playzone.pems.interfaces.rest.auditoria.response.LogAuditoriaResponse;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final ObtenerAuditoriaUseCase obtenerAuditoria;

    @GetMapping
    @PreAuthorize("hasAuthority('auditoria.ver')")
    public ResponseEntity<ApiResponse<PagedResponse<LogAuditoriaResponse>>> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) UUID   idUsuario,
            @RequestParam(required = false) String modulo,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String entidad,
            @RequestParam(required = false) String nivel,
            @RequestParam(required = false) String resultado,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        if (hasta.isBefore(desde)) {
            throw new ValidationException("hasta", "La fecha 'hasta' debe ser igual o posterior a 'desde'.");
        }

        ObtenerAuditoriaUseCase.FiltrosQuery filtros = new ObtenerAuditoriaUseCase.FiltrosQuery(
                desde, hasta, idUsuario, modulo, accion, entidad, nivel, resultado, pagina, tamano);

        Page<LogAuditoriaResponse> page = obtenerAuditoria.listarPorFiltros(filtros)
                .map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('auditoria.ver')")
    public ResponseEntity<ApiResponse<LogAuditoriaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(obtenerAuditoria.obtenerPorId(id))));
    }

    @GetMapping("/usuarios/{idAdmin}")
    @PreAuthorize("hasAuthority('auditoria.ver')")
    public ResponseEntity<ApiResponse<PagedResponse<LogAuditoriaResponse>>> listarPorUsuario(
            @PathVariable UUID idAdmin,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        Page<LogAuditoriaResponse> page = obtenerAuditoria.listarPorUsuario(idAdmin, pagina, tamano)
                .map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(page)));
    }

    @GetMapping("/entidad/{entidad}/{idEntidad}")
    @PreAuthorize("hasAuthority('auditoria.ver')")
    public ResponseEntity<ApiResponse<PagedResponse<LogAuditoriaResponse>>> listarPorEntidad(
            @PathVariable String entidad,
            @PathVariable Long idEntidad,
            @RequestParam(defaultValue = "0")  int pagina,
            @RequestParam(defaultValue = "20") int tamano) {

        Page<LogAuditoriaResponse> page = obtenerAuditoria.listarPorEntidad(entidad, idEntidad, pagina, tamano)
                .map(this::toResponse);

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(page)));
    }

    private LogAuditoriaResponse toResponse(com.playzone.pems.domain.auditoria.model.LogAuditoria log) {
        return LogAuditoriaResponse.builder()
                .id(log.getId())
                .idUsuarioAdmin(log.getIdUsuarioAdmin())
                .nombreUsuario(log.getNombreUsuario())
                .accion(log.getAccion())
                .modulo(log.getModulo())
                .entidadAfectada(log.getEntidadAfectada())
                .idEntidad(log.getIdEntidad())
                .valorAnterior(log.getValorAnterior())
                .valorNuevo(log.getValorNuevo())
                .descripcion(log.getDescripcion())
                .ipOrigen(log.getIpOrigen())
                .userAgent(log.getUserAgent())
                .nivel(log.getNivel())
                .resultado(log.getResultado())
                .fechaLog(log.getFechaLog())
                .build();
    }
}
