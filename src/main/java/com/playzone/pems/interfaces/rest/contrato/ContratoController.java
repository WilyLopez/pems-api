package com.playzone.pems.interfaces.rest.contrato;

import com.playzone.pems.application.contrato.dto.command.CargarContratoCommand;
import com.playzone.pems.application.contrato.dto.query.ArchivoContratoQuery;
import com.playzone.pems.application.contrato.dto.query.ContratoPageQuery;
import com.playzone.pems.application.contrato.dto.query.ContratoQuery;
import com.playzone.pems.application.contrato.port.in.CargarContratoUseCase;
import com.playzone.pems.application.contrato.port.in.DescargarContratoUseCase;
import com.playzone.pems.application.contrato.port.in.ListarContratosUseCase;
import com.playzone.pems.application.contrato.port.in.ObtenerContratoUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.contrato.mapper.ContratoResponseMapper;
import com.playzone.pems.interfaces.rest.contrato.response.ContratoResponse;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import com.playzone.pems.shared.util.SortUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contratos")
@RequiredArgsConstructor
public class ContratoController {

    private final CargarContratoUseCase    cargarUseCase;
    private final DescargarContratoUseCase descargarUseCase;
    private final ObtenerContratoUseCase   obtenerUseCase;
    private final ListarContratosUseCase   listarUseCase;
    private final ContratoResponseMapper   mapper;
    private final SupabaseAuthFacade       supabaseAuthFacade;

    @GetMapping
    @PreAuthorize("hasAuthority('evento.contrato')")
    public ResponseEntity<ApiResponse<PagedResponse<ContratoResponse>>> listar(
            @RequestParam(defaultValue = "0")                  int       page,
            @RequestParam(defaultValue = "15")                 int       size,
            @RequestParam(defaultValue = "fechaCarga,desc")     String    sort,
            @RequestParam(required = false)                    Long      idSede,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                                               LocalDate fechaEvento) {

        String sortProperty = "fechaCarga".equals(sort.split(",")[0]) ? "cargadoAt" : sort.split(",")[0];
        Sort parsedSort = SortUtils.parsearSort(sort);
        Sort finalSort = Sort.by(parsedSort.iterator().next().getDirection(), sortProperty);

        Pageable pageable = PageRequest.of(page, size, finalSort);
        ContratoPageQuery resultado = listarUseCase.ejecutar(idSede, fechaEvento, pageable);

        PagedResponse<ContratoResponse> paginado = PagedResponse.<ContratoResponse>builder()
                .content(resultado.getContent().stream().map(mapper::toResponse).toList())
                .page(resultado.getPage())
                .size(resultado.getSize())
                .totalElements(resultado.getTotalElements())
                .totalPages(resultado.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(paginado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('evento.contrato')")
    public ResponseEntity<ApiResponse<ContratoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(obtenerUseCase.porId(id))));
    }

    @GetMapping("/eventos/{idEvento}")
    @PreAuthorize("hasAuthority('evento.contrato') or @supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<ContratoResponse>> obtenerPorEvento(
            @PathVariable Long idEvento) {
        ContratoQuery query = obtenerUseCase.porEvento(idEvento);
        validarAccesoCliente(query.getIdCliente());
        return ResponseEntity.ok(ApiResponse.ok(mapper.toResponse(query)));
    }

    @PostMapping("/eventos/{idEvento}")
    @PreAuthorize("@supabaseAuthFacade.tieneRol('ADMIN') or @supabaseAuthFacade.tieneRol('SUPERADMIN')")
    public ResponseEntity<ApiResponse<ContratoResponse>> cargar(
            @PathVariable Long idEvento,
            @RequestPart("archivo") MultipartFile archivo) {

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo del contrato", e);
        }

        ContratoQuery query = cargarUseCase.ejecutar(CargarContratoCommand.builder()
                .idEventoPrivado(idEvento)
                .archivo(contenido)
                .contentType(archivo.getContentType())
                .idUsuarioCarga(usuarioActual())
                .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toResponse(query)));
    }

    @GetMapping("/eventos/{idEvento}/descargar")
    @PreAuthorize("hasAuthority('evento.contrato') or @supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<byte[]> descargar(@PathVariable Long idEvento) {
        ContratoQuery query = obtenerUseCase.porEvento(idEvento);
        validarAccesoCliente(query.getIdCliente());

        ArchivoContratoQuery archivo = descargarUseCase.ejecutar(idEvento);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", archivo.getNombreArchivo());

        return new ResponseEntity<>(archivo.getContenido(), headers, HttpStatus.OK);
    }

    private void validarAccesoCliente(Long idCliente) {
        if (!supabaseAuthFacade.tieneRol("CLIENTE")) {
            return;
        }
        Long propio = supabaseAuthFacade.clientePerfilId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Cliente sin perfil asociado"));
        if (!propio.equals(idCliente)) {
            throw new AccessDeniedException("No puedes consultar el contrato de otro cliente");
        }
    }

    private UUID usuarioActual() {
        return supabaseAuthFacade.usuarioActualId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }
}
