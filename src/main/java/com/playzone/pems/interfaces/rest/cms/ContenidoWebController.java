package com.playzone.pems.interfaces.rest.cms;

import com.playzone.pems.application.cms.dto.command.EditarContenidoCommand;
import com.playzone.pems.application.cms.dto.query.ContenidoWebQuery;
import com.playzone.pems.application.cms.port.in.ConsultarContenidoWebUseCase;
import com.playzone.pems.application.cms.port.in.EditarContenidoWebUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.cms.request.EditarContenidoRequest;
import com.playzone.pems.interfaces.rest.cms.response.ContenidoWebResponse;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contenido")
@RequiredArgsConstructor
public class ContenidoWebController {

    private final EditarContenidoWebUseCase    editarUseCase;
    private final ConsultarContenidoWebUseCase consultarUseCase;
    private final SupabaseAuthFacade           supabaseAuthFacade;

    @GetMapping
    @PreAuthorize("hasAuthority('sitio.contenido')")
    public ResponseEntity<ApiResponse<PagedResponse<ContenidoWebResponse>>> listar(
            @RequestParam(required = false) String seccionCodigo,
            @RequestParam(required = false) String clave,
            @PageableDefault(size = 50) Pageable pageable) {
        var page = consultarUseCase.listar(seccionCodigo, clave, pageable);
        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.of(page.map(this::toResponse))));
    }

    @GetMapping("/publico")
    public ResponseEntity<ApiResponse<List<ContenidoWebResponse>>> listarPublico() {
        List<ContenidoWebResponse> contenido = consultarUseCase.listarPublico()
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(contenido));
    }

    @PutMapping("/{idContenido}")
    @PreAuthorize("hasAuthority('sitio.contenido')")
    public ResponseEntity<ApiResponse<ContenidoWebResponse>> editar(
            @PathVariable Long idContenido,
            @Valid @RequestBody EditarContenidoRequest request) {

        ContenidoWebQuery query = editarUseCase.ejecutar(EditarContenidoCommand.builder()
                .idContenido(idContenido)
                .valorEs(request.getValorEs())
                .valorEn(request.getValorEn())
                .imagenUrl(request.getImagenUrl())
                .descripcion(request.getDescripcion())
                .metadatos(request.getMetadatos())
                .visible(request.getVisible())
                .ordenVisualizacion(request.getOrdenVisualizacion())
                .idUsuarioEditor(supabaseAuthFacade.usuarioActualId().orElseThrow())
                .build());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(query)));
    }

    private ContenidoWebResponse toResponse(ContenidoWebQuery q) {
        return ContenidoWebResponse.builder()
                .id(q.getId())
                .seccionCodigo(q.getSeccion())
                .tipoContenidoCodigo(q.getTipoContenido())
                .clave(q.getClave())
                .valorEs(q.getValorEs())
                .valorEn(q.getValorEn())
                .imagenUrl(q.getImagenUrl())
                .descripcion(q.getDescripcion())
                .ordenVisualizacion(q.getOrdenVisualizacion())
                .visible(q.isVisible())
                .version(q.getVersion())
                .metadatos(q.getMetadatos())
                .activo(q.isActivo())
                .fechaActualizacion(q.getFechaActualizacion())
                .build();
    }
}