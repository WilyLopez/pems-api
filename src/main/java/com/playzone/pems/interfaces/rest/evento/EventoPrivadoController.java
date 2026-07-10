package com.playzone.pems.interfaces.rest.evento;

import com.playzone.pems.application.evento.dto.query.KpisEventosQuery;
import com.playzone.pems.application.evento.dto.command.ConfirmarEventoCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarPagoCuotaCommand;
import com.playzone.pems.application.evento.dto.command.RegistrarSaldoCommand;
import com.playzone.pems.application.evento.dto.command.SolicitarEventoPrivadoCommand;
import com.playzone.pems.application.evento.dto.command.VentaPagoItem;
import com.playzone.pems.application.evento.dto.query.ChecklistEventoQuery;
import com.playzone.pems.application.evento.port.in.BuscarEventosAdminUseCase;
import com.playzone.pems.application.evento.port.in.CancelarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.in.CompletarEventoUseCase;
import com.playzone.pems.application.evento.port.in.ConfirmarEventoPrivadoUseCase;
import com.playzone.pems.application.evento.port.in.ConsultarEventosPrivadosUseCase;
import com.playzone.pems.application.evento.port.in.GestionarChecklistUseCase;
import com.playzone.pems.application.evento.port.in.RegistrarPagoCuotaUseCase;
import com.playzone.pems.application.evento.port.in.RegistrarSaldoUseCase;
import com.playzone.pems.application.evento.port.in.SolicitarEventoPrivadoUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.evento.mapper.EventoPrivadoResponseMapper;
import com.playzone.pems.interfaces.rest.evento.request.CancelarEventoRequest;
import com.playzone.pems.interfaces.rest.evento.request.ConfirmarEventoRequest;
import com.playzone.pems.interfaces.rest.evento.request.PagoItemRequest;
import com.playzone.pems.interfaces.rest.evento.request.RegistrarPagoCuotaRequest;
import com.playzone.pems.interfaces.rest.evento.request.RegistrarSaldoRequest;
import com.playzone.pems.interfaces.rest.evento.request.SolicitarEventoPrivadoRequest;
import com.playzone.pems.interfaces.rest.evento.response.EventoPrivadoResponse;
import com.playzone.pems.shared.response.ApiResponse;
import com.playzone.pems.shared.util.SortUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eventos-privados")
@RequiredArgsConstructor
public class EventoPrivadoController {

    private final SolicitarEventoPrivadoUseCase   solicitarUseCase;
    private final ConfirmarEventoPrivadoUseCase   confirmarUseCase;
    private final CancelarEventoPrivadoUseCase    cancelarUseCase;
    private final ConsultarEventosPrivadosUseCase consultarUseCase;
    private final BuscarEventosAdminUseCase       buscarAdminUseCase;
    private final GestionarChecklistUseCase       checklistUseCase;
    private final CompletarEventoUseCase          completarUseCase;
    private final RegistrarSaldoUseCase           registrarSaldoUseCase;
    private final RegistrarPagoCuotaUseCase       registrarPagoCuotaUseCase;
    private final EventoPrivadoResponseMapper     mapper;
    private final SupabaseAuthFacade              supabaseAuthFacade;

    @GetMapping
    @PreAuthorize("hasAuthority('evento.ver') or @supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<Page<EventoPrivadoResponse>>> listar(
            @RequestParam(required = false) Long      idCliente,
            @RequestParam(required = false) Long      idSede,
            @RequestParam(required = false) String    estado,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            Pageable pageable) {

        Long idClienteEfectivo = resolverIdCliente(idCliente);
        Page<EventoPrivadoResponse> result;
        if (idClienteEfectivo != null) {
            result = consultarUseCase.consultarPorCliente(idClienteEfectivo, pageable)
                    .map(mapper::toResponse);
        } else if (idSede != null && inicio != null && fin != null) {
            result = consultarUseCase.consultarPorSedeYRangoFechas(idSede, inicio, fin, pageable)
                    .map(mapper::toResponse);
        } else if (idSede != null && estado != null) {
            result = consultarUseCase.consultarPorSedeYEstado(idSede, estado, pageable)
                    .map(mapper::toResponse);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere idCliente o filtros de sede");
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('evento.ver')")
    public ResponseEntity<ApiResponse<Page<EventoPrivadoResponse>>> buscarAdmin(
            @RequestParam(required = false)                    Long      idSede,
            @RequestParam(required = false)                    String    estado,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false)                    String    tipoEvento,
            @RequestParam(required = false)                    String    modalidadPago,
            @RequestParam(required = false)                    String    search,
            @RequestParam(defaultValue = "0")                  int       page,
            @RequestParam(defaultValue = "15")                 int       size,
            @RequestParam(defaultValue = "fechaEvento,asc")    String    sort) {

        Pageable pageable = PageRequest.of(page, size, SortUtils.parsearSort(sort));
        return ResponseEntity.ok(ApiResponse.ok(
                buscarAdminUseCase.buscar(idSede, estado, fechaDesde, fechaHasta,
                                tipoEvento, modalidadPago, search, pageable)
                        .map(mapper::toResponse)));
    }

    @GetMapping("/admin/kpis")
    @PreAuthorize("hasAuthority('evento.ver')")
    public ResponseEntity<ApiResponse<KpisEventosQuery>> kpis(
            @RequestParam(required = false) Long idSede) {
        return ResponseEntity.ok(ApiResponse.ok(buscarAdminUseCase.kpis(idSede)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('evento.ver') or @supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                mapper.toResponse(consultarUseCase.consultarPorId(id))));
    }

    @PostMapping("/clientes/{idCliente}/sedes/{idSede}")
    @PreAuthorize("hasAuthority('evento.crear') or @supabaseAuthFacade.tieneRol('CLIENTE')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> solicitar(
            @PathVariable Long idCliente,
            @PathVariable Long idSede,
            @Valid @RequestBody SolicitarEventoPrivadoRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(mapper.toResponse(
                        solicitarUseCase.ejecutar(
                                SolicitarEventoPrivadoCommand.builder()
                                        .idCliente(idCliente).idSede(idSede)
                                        .idTurno(request.getIdTurno())
                                        .fechaEvento(request.getFechaEvento())
                                        .tipoEvento(request.getTipoEvento())
                                        .contactoAdicional(request.getContactoAdicional())
                                        .origenContacto(request.getOrigenContacto())
                                        .aforoDeclarado(request.getAforoDeclarado())
                                        .nombreNino(request.getNombreNino())
                                        .edadCumple(request.getEdadCumple())
                                        .idPaquete(request.getIdPaquete())
                                        .idsExtras(request.getIdsExtras())
                                        .extrasLibres(request.getExtrasLibres())
                                        .observaciones(request.getObservaciones())
                                        .descripcionPersonalizada(request.getDescripcionPersonalizada())
                                        .presupuestoEstimado(request.getPresupuestoEstimado())
                                        .idsServiciosCotizacion(request.getIdsServiciosCotizacion())
                                        .esCotizacionPersonalizada(request.isEsCotizacionPersonalizada())
                                        .build()))));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> confirmar(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmarEventoRequest request) {

        List<VentaPagoItem> pagos = resolverPagosAdelanto(request);

        return ResponseEntity.ok(ApiResponse.ok(
                mapper.toResponse(confirmarUseCase.ejecutar(
                        ConfirmarEventoCommand.builder()
                                .idEvento(id)
                                .precioTotal(request.getPrecioTotal())
                                .montoAdelanto(request.getMontoAdelanto())
                                .idUsuarioGestor(usuarioActual())
                                .pagosAdelanto(pagos)
                                .modalidadPago(request.getModalidadPago() != null
                                        ? request.getModalidadPago() : "AL_CONTADO")
                                .numeroCuotas(request.getNumeroCuotas())
                                .fechaLimitePago(request.getFechaLimitePago())
                                .build()))));
    }

    @PostMapping("/{id}/completar")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> completar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                mapper.toResponse(completarUseCase.completar(id, usuarioActual()))));
    }

    @PostMapping("/{id}/registrar-saldo")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> registrarSaldo(
            @PathVariable Long id,
            @Valid @RequestBody RegistrarSaldoRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                mapper.toResponse(registrarSaldoUseCase.registrarSaldo(
                        RegistrarSaldoCommand.builder()
                                .idEvento(id)
                                .monto(request.getMonto())
                                .medioPago(request.getMedioPago())
                                .idUsuario(usuarioActual())
                                .build()))));
    }

    @PostMapping("/{idEvento}/cuotas/{idCuota}/pagar")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> pagarCuota(
            @PathVariable Long idEvento,
            @PathVariable Long idCuota,
            @Valid @RequestBody RegistrarPagoCuotaRequest request) {

        List<VentaPagoItem> pagos = request.getPagos().stream()
                .map(p -> VentaPagoItem.builder()
                        .medioPagoCodigo(p.getMedioPago())
                        .monto(p.getMonto())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                mapper.toResponse(registrarPagoCuotaUseCase.ejecutar(
                        RegistrarPagoCuotaCommand.builder()
                                .idCuota(idCuota)
                                .pagos(pagos)
                                .idUsuario(usuarioActual())
                                .build()))));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<EventoPrivadoResponse>> cancelar(
            @PathVariable Long id,
            @Valid @RequestBody CancelarEventoRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(
                mapper.toResponse(cancelarUseCase.ejecutar(id, request.getMotivo()))));
    }

    @GetMapping("/{idEvento}/checklist")
    @PreAuthorize("hasAuthority('evento.ver')")
    public ResponseEntity<ApiResponse<List<ChecklistEventoQuery>>> listarChecklist(
            @PathVariable Long idEvento) {
        return ResponseEntity.ok(ApiResponse.ok(checklistUseCase.consultarPorEvento(idEvento)));
    }

    @PostMapping("/{idEvento}/checklist/{idChecklist}/completar")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<ChecklistEventoQuery>> completarTarea(
            @PathVariable Long idEvento,
            @PathVariable Long idChecklist) {
        return ResponseEntity.ok(ApiResponse.ok(
                checklistUseCase.completar(idEvento, idChecklist, usuarioActual())));
    }

    @PostMapping("/{idEvento}/checklist/{idChecklist}/descompletar")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<ChecklistEventoQuery>> descompletarTarea(
            @PathVariable Long idEvento,
            @PathVariable Long idChecklist) {
        return ResponseEntity.ok(ApiResponse.ok(checklistUseCase.descompletar(idEvento, idChecklist)));
    }

    @PostMapping("/{idEvento}/checklist")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<ChecklistEventoQuery>> agregarTarea(
            @PathVariable Long idEvento,
            @RequestBody java.util.Map<String, String> body) {
        String tarea = body.get("tarea");
        if (tarea == null || tarea.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El campo tarea es obligatorio.");
        }
        return ResponseEntity.ok(ApiResponse.ok(checklistUseCase.agregarTarea(idEvento, tarea)));
    }

    @DeleteMapping("/{idEvento}/checklist/{idChecklist}")
    @PreAuthorize("hasAuthority('evento.confirmar')")
    public ResponseEntity<ApiResponse<Void>> eliminarTarea(
            @PathVariable Long idEvento,
            @PathVariable Long idChecklist) {
        checklistUseCase.eliminarTarea(idEvento, idChecklist);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Construye la lista de VentaPagoItem combinando el campo nuevo (pagosAdelanto)
     * y el legado (medioPago), para mantener compatibilidad con el frontend actual.
     */
    private List<VentaPagoItem> resolverPagosAdelanto(ConfirmarEventoRequest request) {
        if (request.getPagosAdelanto() != null && !request.getPagosAdelanto().isEmpty()) {
            return request.getPagosAdelanto().stream()
                    .map(p -> VentaPagoItem.builder()
                            .medioPagoCodigo(p.getMedioPago())
                            .monto(p.getMonto())
                            .build())
                    .toList();
        }
        BigDecimal adelanto = request.getMontoAdelanto();
        if (adelanto != null && adelanto.compareTo(BigDecimal.ZERO) > 0 && request.getMedioPago() != null) {
            return List.of(VentaPagoItem.builder()
                    .medioPagoCodigo(request.getMedioPago())
                    .monto(adelanto)
                    .build());
        }
        return List.of();
    }

    private UUID usuarioActual() {
        return supabaseAuthFacade.usuarioActualId()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Usuario no autenticado"));
    }

    private Long resolverIdCliente(Long solicitado) {
        if (supabaseAuthFacade.tieneRol("CLIENTE")) {
            Long propio = supabaseAuthFacade.clientePerfilId()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Cliente sin perfil asociado"));
            if (solicitado != null && !solicitado.equals(propio)) {
                throw new AccessDeniedException("No puedes consultar datos de otro cliente");
            }
            return propio;
        }
        return solicitado;
    }
}
