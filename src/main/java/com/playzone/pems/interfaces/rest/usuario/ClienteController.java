package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.application.usuario.dto.command.ActualizarClientePerfilCommand;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.application.usuario.dto.command.CompletarPerfilClienteCommand;
import com.playzone.pems.application.usuario.dto.command.RegistrarClientePerfilCommand;
import com.playzone.pems.application.usuario.dto.command.RegistrarClientePublicoCommand;
import com.playzone.pems.application.usuario.dto.query.ClientePerfilQuery;
import com.playzone.pems.application.usuario.port.in.ActualizarClientePerfilUseCase;
import com.playzone.pems.application.usuario.port.in.ActualizarSegmentoPerfilUseCase;
import com.playzone.pems.application.usuario.port.in.CompletarPerfilClienteUseCase;
import com.playzone.pems.application.usuario.port.in.ActivarClientePerfilUseCase;
import com.playzone.pems.application.usuario.port.in.ConfirmarCambioCorreoUseCase;
import com.playzone.pems.application.usuario.port.in.DesactivarClientePerfilUseCase;
import com.playzone.pems.application.usuario.port.in.HacerVipPerfilUseCase;
import com.playzone.pems.application.usuario.port.in.ListarClientesPerfilUseCase;
import com.playzone.pems.application.usuario.port.in.ObtenerClientePerfilUseCase;
import com.playzone.pems.application.usuario.port.in.QuitarVipPerfilUseCase;
import com.playzone.pems.application.cms.port.in.RegistrarConsentimientoUseCase;
import lombok.extern.slf4j.Slf4j;
import com.playzone.pems.application.usuario.port.in.RegistrarClientePerfilUseCase;
import com.playzone.pems.application.usuario.port.in.RegistrarClientePublicoUseCase;
import com.playzone.pems.application.usuario.port.in.RegistrarVisitaPerfilUseCase;
import com.playzone.pems.domain.usuario.model.ClientePerfil;
import com.playzone.pems.infrastructure.security.SupabaseAuthContext;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.usuario.request.ActualizarClientePerfilRequest;
import com.playzone.pems.interfaces.rest.usuario.request.CompletarPerfilClienteRequest;
import com.playzone.pems.interfaces.rest.usuario.request.ConfirmarCambioCorreoRequest;
import com.playzone.pems.interfaces.rest.usuario.request.HacerVipRequest;
import com.playzone.pems.interfaces.rest.usuario.request.RegistrarClientePerfilRequest;
import com.playzone.pems.interfaces.rest.usuario.request.RegistrarClientePublicoRequest;
import com.playzone.pems.interfaces.rest.usuario.response.ClientePerfilResponse;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/clientes")
public class ClienteController {

    private final RegistrarClientePerfilUseCase  registrarUseCase;
    private final RegistrarClientePublicoUseCase registrarPublicoUseCase;
    private final RegistrarConsentimientoUseCase consentimientoUseCase;
    private final ActualizarClientePerfilUseCase actualizarUseCase;
    private final ListarClientesPerfilUseCase    listarUseCase;
    private final ObtenerClientePerfilUseCase    obtenerUseCase;
    private final ActivarClientePerfilUseCase    activarUseCase;
    private final DesactivarClientePerfilUseCase desactivarUseCase;
    private final HacerVipPerfilUseCase          hacerVipUseCase;
    private final QuitarVipPerfilUseCase         quitarVipUseCase;
    private final RegistrarVisitaPerfilUseCase   visitaUseCase;
    private final ActualizarSegmentoPerfilUseCase segmentoUseCase;
    private final CompletarPerfilClienteUseCase  completarUseCase;
    private final ConfirmarCambioCorreoUseCase   confirmarCambioCorreoUseCase;
    private final SupabaseAuthFacade             supabaseAuthFacade;
    private final StoragePort storagePort;
    private final String bucketPublico;

    public ClienteController(
            RegistrarClientePerfilUseCase registrarUseCase,
            RegistrarClientePublicoUseCase registrarPublicoUseCase,
            RegistrarConsentimientoUseCase consentimientoUseCase,
            ActualizarClientePerfilUseCase actualizarUseCase,
            ListarClientesPerfilUseCase listarUseCase,
            ObtenerClientePerfilUseCase obtenerUseCase,
            ActivarClientePerfilUseCase activarUseCase,
            DesactivarClientePerfilUseCase desactivarUseCase,
            HacerVipPerfilUseCase hacerVipUseCase,
            QuitarVipPerfilUseCase quitarVipUseCase,
            RegistrarVisitaPerfilUseCase visitaUseCase,
            ActualizarSegmentoPerfilUseCase segmentoUseCase,
            CompletarPerfilClienteUseCase completarUseCase,
            ConfirmarCambioCorreoUseCase confirmarCambioCorreoUseCase,
            SupabaseAuthFacade supabaseAuthFacade,
            StoragePort storagePort,
            @Value("${supabase.storage.bucket-publico}") String bucketPublico) {
        this.registrarUseCase = registrarUseCase;
        this.registrarPublicoUseCase = registrarPublicoUseCase;
        this.consentimientoUseCase = consentimientoUseCase;
        this.actualizarUseCase = actualizarUseCase;
        this.listarUseCase = listarUseCase;
        this.obtenerUseCase = obtenerUseCase;
        this.activarUseCase = activarUseCase;
        this.desactivarUseCase = desactivarUseCase;
        this.hacerVipUseCase = hacerVipUseCase;
        this.quitarVipUseCase = quitarVipUseCase;
        this.visitaUseCase = visitaUseCase;
        this.segmentoUseCase = segmentoUseCase;
        this.completarUseCase = completarUseCase;
        this.confirmarCambioCorreoUseCase = confirmarCambioCorreoUseCase;
        this.supabaseAuthFacade = supabaseAuthFacade;
        this.storagePort = storagePort;
        this.bucketPublico = bucketPublico;
    }

    @PostMapping("/registro")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> registrarPublico(
            @Valid @RequestBody RegistrarClientePublicoRequest request) {

        ClientePerfil perfil = registrarPublicoUseCase.ejecutar(
                RegistrarClientePublicoCommand.builder()
                        .nombre(request.getNombre())
                        .correo(request.getCorreo())
                        .password(request.getPassword())
                        .telefono(request.getTelefono())
                        .tipoDocumentoCodigo(request.getTipoDocumento())
                        .numeroDocumento(request.getNumeroDocumento())
                        .build());

        try {
            consentimientoUseCase.registrar(
                    "REGISTRO", perfil.getId(), java.util.List.of("TERMINOS", "PRIVACIDAD"));
        } catch (Exception e) {
            log.warn("No se pudo registrar el consentimiento del registro del cliente {}: {}",
                    perfil.getId(), e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(perfil)));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('cliente.crear')")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> registrarAdmin(
            @Valid @RequestBody RegistrarClientePerfilRequest request) {

        ClientePerfil perfil = registrarUseCase.ejecutar(
                RegistrarClientePerfilCommand.builder()
                        .tipoDocumentoCodigo(request.getTipoDocumentoCodigo())
                        .numeroDocumento(request.getNumeroDocumento())
                        .nombres(request.getNombres())
                        .apellidoPaterno(request.getApellidoPaterno())
                        .apellidoMaterno(request.getApellidoMaterno())
                        .correo(request.getCorreo())
                        .telefono(request.getTelefono())
                        .origen(request.getOrigen() != null ? request.getOrigen() : "ADMIN")
                        .aceptaComunicaciones(request.isAceptaComunicaciones())
                        .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(perfil)));
    }

    @PostMapping("/me/completar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> completarPerfil(
            @Valid @RequestBody CompletarPerfilClienteRequest request) {

        SupabaseAuthContext ctx = supabaseAuthFacade.contextoActual()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"));

        ClientePerfil perfil = completarUseCase.ejecutar(
                CompletarPerfilClienteCommand.builder()
                        .usuarioId(ctx.userId())
                        .correo(ctx.email())
                        .nombres(request.getNombres())
                        .apellidoPaterno(request.getApellidoPaterno())
                        .apellidoMaterno(request.getApellidoMaterno())
                        .tipoDocumentoCodigo(request.getTipoDocumento())
                        .numeroDocumento(request.getNumeroDocumento())
                        .telefono(request.getTelefono())
                        .aceptaComunicaciones(request.isAceptaComunicaciones())
                        .build());

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(perfil)));
    }

    @PostMapping("/me/correo/confirmar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> confirmarCambioCorreo(
            @Valid @RequestBody ConfirmarCambioCorreoRequest request) {

        Long clienteId = supabaseAuthFacade.clientePerfilId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No autenticado"));

        confirmarCambioCorreoUseCase.confirmar(clienteId, request.getToken());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('cliente.ver')")
    public ResponseEntity<ApiResponse<PagedResponse<ClientePerfilResponse>>> listar(
            @RequestParam(defaultValue = "0")              int     page,
            @RequestParam(defaultValue = "15")             int     size,
            @RequestParam(defaultValue = "createdAt,desc")  String  sort,
            @RequestParam(required = false)                String  search,
            @RequestParam(required = false)                Boolean esVip,
            @RequestParam(required = false)                Boolean activo,
            @RequestParam(required = false)                Boolean frecuente,
            @RequestParam(required = false)                Boolean aceptaComunicaciones,
            @RequestParam(required = false)                String  origen,
            @RequestParam(required = false)                String  segmentoCodigo) {

        String[]       parts = sort.split(",");
        Sort.Direction dir   = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        ClientePerfilQuery query = ClientePerfilQuery.builder()
                .search(search)
                .esVip(esVip)
                .activo(activo)
                .frecuente(frecuente)
                .aceptaComunicaciones(aceptaComunicaciones)
                .origen(origen)
                .segmentoCodigo(segmentoCodigo)
                .build();

        Page<ClientePerfil> pagina = listarUseCase.ejecutar(
                query, PageRequest.of(page, size, Sort.by(dir, parts[0])));

        PagedResponse<ClientePerfilResponse> paginado = PagedResponse.<ClientePerfilResponse>builder()
                .content(pagina.getContent().stream().map(this::toResponse).toList())
                .page(pagina.getNumber())
                .size(pagina.getSize())
                .totalElements(pagina.getTotalElements())
                .totalPages(pagina.getTotalPages())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(paginado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('cliente.ver') and (#id == @supabaseAuthFacade.clientePerfilId().orElse(-1L) or hasAuthority('usuario.gestionar'))")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(obtenerUseCase.ejecutar(id))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('cliente.editar') and (#id == @supabaseAuthFacade.clientePerfilId().orElse(-1L) or hasAuthority('usuario.gestionar'))")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarClientePerfilRequest request) {

        ClientePerfil perfil = actualizarUseCase.ejecutar(id,
                ActualizarClientePerfilCommand.builder()
                        .nombres(request.getNombres())
                        .apellidoPaterno(request.getApellidoPaterno())
                        .apellidoMaterno(request.getApellidoMaterno())
                        .telefono(request.getTelefono())
                        .numeroDocumento(request.getNumeroDocumento())
                        .correo(request.getCorreo())
                        .aceptaComunicaciones(request.getAceptaComunicaciones())
                        .build());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(perfil)));
    }

    @PostMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('cliente.editar')")
    public ResponseEntity<ApiResponse<Void>> activar(@PathVariable Long id) {
        activarUseCase.activar(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('cliente.editar')")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Long id) {
        desactivarUseCase.desactivar(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/vip")
    @PreAuthorize("hasAuthority('cliente.editar')")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> hacerVip(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) HacerVipRequest request) {

        int descuento = (request != null) ? request.getDescuento() : 10;
        return ResponseEntity.ok(
                ApiResponse.ok(toResponse(hacerVipUseCase.ejecutar(id, BigDecimal.valueOf(descuento)))));
    }

    @DeleteMapping("/{id}/vip")
    @PreAuthorize("hasAuthority('cliente.editar')")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> quitarVip(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok(toResponse(quitarVipUseCase.quitarVip(id))));
    }

    @PostMapping("/{id}/visitas")
    @PreAuthorize("hasAuthority('cliente.editar')")
    public ResponseEntity<ApiResponse<Void>> registrarVisita(@PathVariable Long id) {
        visitaUseCase.ejecutarVisita(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/segmento")
    @PreAuthorize("hasAuthority('cliente.editar')")
    public ResponseEntity<ApiResponse<Void>> actualizarSegmento(
            @PathVariable Long id,
            @RequestParam String segmento) {
        segmentoUseCase.ejecutar(id, segmento);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PutMapping("/{id}/foto")
    @PreAuthorize("hasAuthority('cliente.editar') and (#id == @supabaseAuthFacade.clientePerfilId().orElse(-1L) or hasAuthority('usuario.gestionar'))")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> subirFoto(
            @PathVariable Long id,
            @RequestPart("foto") MultipartFile foto) {

        if (foto.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        ClientePerfil perfil = obtenerUseCase.ejecutar(id);

        try {
            byte[] bytes = foto.getBytes();
            String tipoMime = foto.getContentType();
            String nombreOriginal = foto.getOriginalFilename();
            String key = "clientes/fotos/" + UUID.randomUUID() + "_" + (nombreOriginal != null ? nombreOriginal.replaceAll("[^a-zA-Z0-9.\\-]", "_") : "foto");
            String path = storagePort.upload(bucketPublico, key, bytes, tipoMime);

            ClientePerfil actualizado = actualizarUseCase.ejecutar(id,
                    ActualizarClientePerfilCommand.builder()
                            .fotoPerfilPath(path)
                            .actualizarFotoPerfil(true)
                            .build());

            // Eliminar foto anterior si existía
            if (perfil.getFotoPerfilPath() != null) {
                storagePort.deleteByUrl(perfil.getFotoPerfilPath());
            }

            return ResponseEntity.ok(ApiResponse.ok(toResponse(actualizado)));
        } catch (Exception e) {
            throw new RuntimeException("Error al subir la foto de perfil", e);
        }
    }

    @DeleteMapping("/{id}/foto")
    @PreAuthorize("hasAuthority('cliente.editar') and (#id == @supabaseAuthFacade.clientePerfilId().orElse(-1L) or hasAuthority('usuario.gestionar'))")
    public ResponseEntity<ApiResponse<ClientePerfilResponse>> eliminarFoto(@PathVariable Long id) {
        ClientePerfil perfil = obtenerUseCase.ejecutar(id);

        if (perfil.getFotoPerfilPath() != null) {
            storagePort.deleteByUrl(perfil.getFotoPerfilPath());
        }

        ClientePerfil actualizado = actualizarUseCase.ejecutar(id,
                ActualizarClientePerfilCommand.builder()
                        .fotoPerfilPath(null)
                        .actualizarFotoPerfil(true)
                        .build());

        return ResponseEntity.ok(ApiResponse.ok(toResponse(actualizado)));
    }

    private ClientePerfilResponse toResponse(ClientePerfil p) {
        return ClientePerfilResponse.builder()
                .id(p.getId())
                .tipoDocumentoCodigo(p.getTipoDocumentoCodigo())
                .numeroDocumento(p.getNumeroDocumento())
                .nombres(p.getNombres())
                .apellidoPaterno(p.getApellidoPaterno())
                .apellidoMaterno(p.getApellidoMaterno())
                .nombreCompleto(p.nombreCompleto())
                .correo(p.getCorreo())
                .telefono(p.getTelefono())
                .esVip(p.isEsVip())
                .descuentoVip(p.getDescuentoVip())
                .contadorVisitas(p.getContadorVisitas())
                .ultimaVisitaAt(p.getUltimaVisitaAt())
                .totalGastado(p.getTotalGastado())
                .segmentoCodigo(p.getSegmentoCodigo())
                .origen(p.getOrigen())
                .aceptaComunicaciones(p.isAceptaComunicaciones())
                .creadoEn(p.getCreatedAt())
                .fotoPerfilPath(p.getFotoPerfilPath())
                .build();
    }
}
