package com.playzone.pems.interfaces.rest.marketing;

import com.playzone.pems.application.marketing.dto.command.ActualizarCorreoMarketingCommand;
import com.playzone.pems.application.marketing.dto.command.CrearCampanaCommand;
import com.playzone.pems.application.marketing.dto.command.CrearCorreoMarketingCommand;
import com.playzone.pems.application.marketing.dto.command.CrearTipoEmailCommand;
import com.playzone.pems.application.marketing.dto.command.FiltroDestinatariosCommand;
import com.playzone.pems.application.marketing.dto.command.GuardarPlantillaCommand;
import com.playzone.pems.application.marketing.dto.query.CampanaEmailQuery;
import com.playzone.pems.application.marketing.dto.query.EnvioEmailQuery;
import com.playzone.pems.application.marketing.dto.query.PlantillaEmailQuery;
import com.playzone.pems.application.marketing.dto.query.TipoEmailQuery;
import com.playzone.pems.application.marketing.port.in.CrearCampanaEmailUseCase;
import com.playzone.pems.application.marketing.port.in.CrearPlantillaEmailUseCase;
import com.playzone.pems.application.marketing.port.in.DesuscribirClienteUseCase;
import com.playzone.pems.application.marketing.port.in.EnviarCampanaUseCase;
import com.playzone.pems.application.marketing.port.in.ListarCampanasUseCase;
import com.playzone.pems.application.marketing.port.in.ListarEnviosUseCase;
import com.playzone.pems.application.marketing.port.in.ListarPlantillasUseCase;
import com.playzone.pems.application.marketing.service.MarketingService;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.marketing.request.ActualizarCorreoMarketingRequest;
import com.playzone.pems.interfaces.rest.marketing.request.CrearCampanaRequest;
import com.playzone.pems.interfaces.rest.marketing.request.CrearCorreoMarketingRequest;
import com.playzone.pems.interfaces.rest.marketing.request.CrearTipoEmailRequest;
import com.playzone.pems.interfaces.rest.marketing.request.EnviarCampanaRequest;
import com.playzone.pems.interfaces.rest.marketing.request.GuardarPlantillaRequest;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.ratelimit.RateLimited;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.response.PagedResponse;
import com.playzone.pems.shared.util.TokenEncryptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketing")
@RequiredArgsConstructor
public class MarketingController {

    private final CrearPlantillaEmailUseCase crearPlantillaUseCase;
    private final ListarPlantillasUseCase    listarPlantillasUseCase;
    private final CrearCampanaEmailUseCase   crearCampanaUseCase;
    private final ListarCampanasUseCase      listarCampanasUseCase;
    private final EnviarCampanaUseCase       enviarCampanaUseCase;
    private final ListarEnviosUseCase        listarEnviosUseCase;
    private final DesuscribirClienteUseCase  desuscribirClienteUseCase;
    private final MarketingService           marketingService;
    private final SupabaseAuthFacade         supabaseAuthFacade;
    private final TokenEncryptor             tokenEncryptor;

    @GetMapping("/tipos-email")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<List<TipoEmailQuery>>> listarTipos() {
        return ResponseEntity.ok(ApiResponse.ok(marketingService.listarTipos()));
    }

    @PostMapping("/tipos-email")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<TipoEmailQuery>> crearTipo(
            @Valid @RequestBody CrearTipoEmailRequest request) {

        TipoEmailQuery query = marketingService.crearTipo(
                CrearTipoEmailCommand.builder()
                        .codigo(request.getCodigo())
                        .nombre(request.getNombre())
                        .descripcion(request.getDescripcion())
                        .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(query));
    }

    @DeleteMapping("/tipos-email/{codigo}")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<Void>> eliminarTipo(@PathVariable String codigo) {
        marketingService.eliminarTipo(codigo);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/plantillas")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<PlantillaEmailQuery>> crearPlantilla(
            @Valid @RequestBody GuardarPlantillaRequest request) {

        PlantillaEmailQuery query = crearPlantillaUseCase.ejecutar(
                GuardarPlantillaCommand.builder()
                        .tipoEmailCodigo(request.getTipoEmailCodigo())
                        .nombre(request.getNombre())
                        .asunto(request.getAsunto())
                        .contenidoHtml(request.getContenidoHtml())
                        .contenidoFallback(request.getContenidoFallback())
                        .variablesPermitidas(request.getVariablesPermitidas())
                        .build(),
                supabaseAuthFacade.usuarioActualId().orElseThrow(() -> new IllegalStateException("Sin usuario autenticado en contexto")));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(query));
    }

    @GetMapping("/plantillas")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<List<PlantillaEmailQuery>>> listarPlantillas(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        List<PlantillaEmailQuery> lista = listarPlantillasUseCase.listarPlantillas(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));

        return ResponseEntity.ok(ApiResponse.ok(lista));
    }

    @PostMapping("/campanas")
    @PreAuthorize("hasAuthority('marketing.campana')")
    public ResponseEntity<ApiResponse<CampanaEmailQuery>> crearCampana(
            @Valid @RequestBody CrearCampanaRequest request) {

        CampanaEmailQuery query = crearCampanaUseCase.ejecutar(
                CrearCampanaCommand.builder()
                        .nombre(request.getNombre())
                        .descripcion(request.getDescripcion())
                        .idPlantillaEmail(request.getIdPlantillaEmail())
                        .fechaProgramada(request.getFechaProgramada())
                        .build(),
                supabaseAuthFacade.usuarioActualId().orElseThrow(() -> new IllegalStateException("Sin usuario autenticado en contexto")));

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(query));
    }

    @GetMapping("/campanas")
    @PreAuthorize("hasAuthority('marketing.campana')")
    public ResponseEntity<ApiResponse<PagedResponse<CampanaEmailQuery>>> listarCampanas(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "15") int size) {

        PagedResponse<CampanaEmailQuery> resultado = listarCampanasUseCase.listarCampanas(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(ApiResponse.ok(resultado));
    }

    @GetMapping("/campanas/{id}")
    @PreAuthorize("hasAuthority('marketing.campana')")
    public ResponseEntity<ApiResponse<CampanaEmailQuery>> getCampana(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marketingService.getCampanaById(id)));
    }

    @PostMapping("/campanas/{id}/enviar")
    @PreAuthorize("hasAuthority('marketing.enviar')")
    public ResponseEntity<ApiResponse<Void>> enviarCampana(
            @PathVariable Long id,
            @RequestBody(required = false) EnviarCampanaRequest request) {

        FiltroDestinatariosCommand filtro = request != null
                ? FiltroDestinatariosCommand.builder()
                        .soloVip(request.getSoloVip())
                        .soloFrecuentes(request.getSoloFrecuentes())
                        .soloNuevos(request.getSoloNuevos())
                        .soloInactivos(request.getSoloInactivos())
                        .soloCorporativos(request.getSoloCorporativos())
                        .soloConAccesoWeb(request.getSoloConAccesoWeb())
                        .soloPresenciales(request.getSoloPresenciales())
                        .valoresVariables(request.getValoresVariables())
                        .build()
                : FiltroDestinatariosCommand.builder().build();

        enviarCampanaUseCase.ejecutar(id, filtro);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/campanas/{id}/variables-requeridas")
    @PreAuthorize("hasAuthority('marketing.campana')")
    public ResponseEntity<ApiResponse<Set<String>>> variablesRequeridas(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marketingService.obtenerVariablesRequeridas(id)));
    }

    @GetMapping("/campanas/{id}/destinatarios/count")
    @PreAuthorize("hasAuthority('marketing.campana')")
    public ResponseEntity<ApiResponse<Integer>> contarDestinatarios(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean soloVip,
            @RequestParam(required = false) Boolean soloFrecuentes,
            @RequestParam(required = false) Boolean soloNuevos,
            @RequestParam(required = false) Boolean soloInactivos,
            @RequestParam(required = false) Boolean soloCorporativos,
            @RequestParam(required = false) Boolean soloConAccesoWeb,
            @RequestParam(required = false) Boolean soloPresenciales) {

        FiltroDestinatariosCommand filtro = FiltroDestinatariosCommand.builder()
                .soloVip(soloVip)
                .soloFrecuentes(soloFrecuentes)
                .soloNuevos(soloNuevos)
                .soloInactivos(soloInactivos)
                .soloCorporativos(soloCorporativos)
                .soloConAccesoWeb(soloConAccesoWeb)
                .soloPresenciales(soloPresenciales)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(marketingService.contarDestinatarios(id, filtro)));
    }

    @GetMapping("/unsubscribe")
    @RateLimited(requests = 10, durationInSeconds = 60)
    public ResponseEntity<String> unsubscribe(@RequestParam String token) {
        Long idCliente;
        try {
            idCliente = Long.valueOf(tokenEncryptor.decrypt(token));
        } catch (Exception e) {
            throw new ValidationException("token", "El enlace de baja no es válido.");
        }

        desuscribirClienteUseCase.ejecutar(idCliente);

        String html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<title>Baja de comunicaciones</title></head>"
                + "<body style=\"font-family:Arial,sans-serif;text-align:center;padding:60px 20px;color:#1A1A2E;\">"
                + "<h2>Listo, te diste de baja</h2>"
                + "<p>Ya no recibirás más correos de marketing de Kiki y Lala.</p>"
                + "</body></html>";

        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    @GetMapping("/correos")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<PagedResponse<PlantillaEmailQuery>>> listarCorreos(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<PlantillaEmailQuery> resultado = marketingService.listarCorreosMarketing(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return ResponseEntity.ok(ApiResponse.ok(resultado));
    }

    @GetMapping("/correos/{id}")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<PlantillaEmailQuery>> getCorreo(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marketingService.getCorreoMarketingById(id)));
    }

    @PostMapping("/correos")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<PlantillaEmailQuery>> crearCorreo(
            @Valid @RequestBody CrearCorreoMarketingRequest request) {

        UUID idUsuario = supabaseAuthFacade.usuarioActualId()
                .orElseThrow(() -> new IllegalStateException("Sin usuario autenticado en contexto"));

        PlantillaEmailQuery query = marketingService.crearCorreoMarketing(
                CrearCorreoMarketingCommand.builder()
                        .tipoEmailCodigo(request.getTipoEmailCodigo())
                        .nombre(request.getNombre())
                        .asunto(request.getAsunto())
                        .contenidoBloques(request.getContenidoBloques())
                        .variablesPermitidas(request.getVariablesPermitidas())
                        .contenidoFallback(request.getContenidoFallback())
                        .build(),
                idUsuario);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(query));
    }

    @PutMapping("/correos/{id}")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<PlantillaEmailQuery>> actualizarCorreo(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarCorreoMarketingRequest request) {

        UUID idUsuario = supabaseAuthFacade.usuarioActualId()
                .orElseThrow(() -> new IllegalStateException("Sin usuario autenticado en contexto"));

        PlantillaEmailQuery query = marketingService.actualizarCorreoMarketing(
                ActualizarCorreoMarketingCommand.builder()
                        .id(id)
                        .nombre(request.getNombre())
                        .asunto(request.getAsunto())
                        .contenidoBloques(request.getContenidoBloques())
                        .variablesPermitidas(request.getVariablesPermitidas())
                        .contenidoFallback(request.getContenidoFallback())
                        .build(),
                idUsuario);
        return ResponseEntity.ok(ApiResponse.ok(query));
    }

    @PatchMapping("/correos/{id}/estado")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<Void>> toggleCorreo(
            @PathVariable Long id,
            @RequestParam boolean activa) {

        marketingService.toggleCorreoMarketing(id, activa);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @DeleteMapping("/correos/{id}")
    @PreAuthorize("hasAuthority('marketing.plantilla')")
    public ResponseEntity<ApiResponse<Void>> eliminarCorreo(@PathVariable Long id) {
        marketingService.eliminarCorreoMarketing(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/campanas/{id}/envios")
    @PreAuthorize("hasAuthority('marketing.campana')")
    public ResponseEntity<ApiResponse<PagedResponse<EnvioEmailQuery>>> listarEnvios(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        PagedResponse<EnvioEmailQuery> resultado = listarEnviosUseCase.ejecutar(
                id, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return ResponseEntity.ok(ApiResponse.ok(resultado));
    }

}
