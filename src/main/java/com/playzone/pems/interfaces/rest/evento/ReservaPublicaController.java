package com.playzone.pems.interfaces.rest.evento;

import com.playzone.pems.application.evento.dto.command.CrearReservaPublicaCommand;
import com.playzone.pems.application.evento.dto.command.ReprogramarReservaCommand;
import com.playzone.pems.application.evento.dto.query.MetricasReservaQuery;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.application.evento.dto.query.TicketDetalleQuery;
import com.playzone.pems.application.cms.port.in.RegistrarConsentimientoUseCase;
import com.playzone.pems.application.evento.port.in.*;
import com.playzone.pems.application.evento.service.ReservaAdminService;
import com.playzone.pems.application.evento.service.ReservaPublicaService;
import com.playzone.pems.application.notificacion.dto.query.EstadoEntregaQuery;
import com.playzone.pems.application.notificacion.port.in.ConsultarEstadoEntregaUseCase;
import com.playzone.pems.interfaces.rest.evento.request.CrearReservaRequest;
import com.playzone.pems.interfaces.rest.evento.request.ReprogramarReservaRequest;
import com.playzone.pems.interfaces.rest.evento.response.EstadoEntregaCorreoResponse;
import com.playzone.pems.interfaces.rest.evento.response.MetricasReservaResponse;
import com.playzone.pems.interfaces.rest.evento.response.ReservaPublicaResponse;
import com.playzone.pems.interfaces.rest.evento.response.TicketDetalleResponse;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.ratelimit.RateLimited;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import com.playzone.pems.domain.usuario.repository.SedeRepository;
import com.playzone.pems.infrastructure.pdf.TicketIngresoPdfService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.playzone.pems.domain.evento.model.enums.EstadoReservaPublica;
import java.util.Arrays;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
public class ReservaPublicaController {

    private final CrearReservaPublicaUseCase  crearUseCase;
    private final RegistrarConsentimientoUseCase consentimientoUseCase;
    private final ReprogramarReservaUseCase   reprogramarUseCase;
    private final CancelarReservaUseCase      cancelarUseCase;
    private final ConsultarReservasUseCase    consultarUseCase;
    private final BuscarReservasAdminUseCase  buscarAdminUseCase;
    private final ConfirmarIngresoUseCase     ingresoUseCase;
    private final ConsultarEstadoEntregaUseCase estadoEntregaUseCase;
    private final ReservaPublicaService       reservaService;
    private final ReservaAdminService         reservaAdminService;
    private final SupabaseAuthFacade          supabaseAuthFacade;
    private final TicketIngresoPdfService     ticketIngresoPdfService;
    private final SedeRepository              sedeRepository;
    private final SedeScopeValidator          sedeScope;

    @GetMapping("/catalogos/estados")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<List<EstadoReservaResponse>>> listarEstados() {
        List<EstadoReservaResponse> estados = Arrays.stream(EstadoReservaPublica.values())
                .map(e -> new EstadoReservaResponse(e.getCodigo(), e.getDescripcion()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(estados));
    }

    public record EstadoReservaResponse(String nombre, String descripcion) {}

    @GetMapping
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<Page<ReservaPublicaResponse>>> listar(
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) Long idSede,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            Pageable pageable) {

        Long idClienteEfectivo = resolverIdCliente(idCliente);
        Page<ReservaPublicaQuery> result;
        if (idClienteEfectivo != null) {
            result = consultarUseCase.consultarPorCliente(idClienteEfectivo, pageable);
        } else if (idSede != null && fecha != null) {
            sedeScope.validarAcceso(idSede);
            result = consultarUseCase.consultarPorSedeYFecha(idSede, fecha, pageable);
        } else if (idSede != null && estado != null) {
            sedeScope.validarAcceso(idSede);
            result = consultarUseCase.consultarPorSedeYEstado(idSede, estado, pageable);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requiere idCliente o filtros de sede");
        }
        return ResponseEntity.ok(ApiResponse.ok(result.map(this::toResponse)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<Page<ReservaPublicaResponse>>> buscarAdmin(
            @RequestParam(required = false)                    Long      idSede,
            @RequestParam(required = false)                    String    estado,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @RequestParam(required = false)                    Boolean   ingresado,
            @RequestParam(required = false)                    Boolean   esReprogramacion,
            @RequestParam(required = false)                    String    medioPago,
            @RequestParam(required = false)                    String    search,
            @RequestParam(defaultValue = "0")                  int       page,
            @RequestParam(defaultValue = "20")                 int       size,
            @RequestParam(defaultValue = "fechaEvento,asc")    String    sort) {

        String[]       parts    = sort.split(",");
        Sort.Direction dir      = parts.length > 1 && "desc".equalsIgnoreCase(parts[1])
                                  ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable       pageable = PageRequest.of(page, size, Sort.by(dir, parts[0]));

        return ResponseEntity.ok(ApiResponse.ok(
                buscarAdminUseCase.buscar(idSede, estado, fecha, ingresado, esReprogramacion, medioPago, search, pageable)
                        .map(this::toResponse)));
    }

    @GetMapping("/admin/metricas")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<MetricasReservaResponse>> metricas(
            @RequestParam(required = false) Long idSede,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {

        MetricasReservaQuery q = buscarAdminUseCase.metricas(idSede, fecha);
        return ResponseEntity.ok(ApiResponse.ok(MetricasReservaResponse.builder()
                .fecha(q.getFecha())
                .totalReservas(q.getTotalReservas())
                .pendientes(q.getPendientes())
                .confirmadas(q.getConfirmadas())
                .canceladas(q.getCanceladas())
                .ingresados(q.getIngresados())
                .aforoMaximo(q.getAforoMaximo())
                .aforoOcupado(q.getAforoOcupado())
                .aforoRestante(q.getAforoRestante())
                .ingresosDia(q.getIngresosDia())
                .build()));
    }

    @GetMapping("/ticket/{numeroTicket}")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> obtenerPorTicket(
            @PathVariable String numeroTicket) {
        ReservaPublicaQuery query = reservaService.consultarPorNumeroTicket(numeroTicket);
        sedeScope.validarAcceso(query.getIdSede());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(query)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> obtenerPorId(
            @PathVariable Long id) {
        ReservaPublicaQuery query = reservaService.consultarPorId(id);
        sedeScope.validarAcceso(query.getIdSede());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(query)));
    }

    @PostMapping("/clientes/{idCliente}/sedes/{idSede}")
    @PreAuthorize("hasAuthority('reserva.crear')")
    @RateLimited(requests = 5, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> crear(
            @PathVariable Long idCliente,
            @PathVariable Long idSede,
            @Valid @RequestBody CrearReservaRequest request) {

        if (!supabaseAuthFacade.tieneRol("CLIENTE")) {
            sedeScope.validarAcceso(idSede);
        }

        ReservaPublicaQuery query = crearUseCase.ejecutar(
                CrearReservaPublicaCommand.builder()
                        .idCliente(idCliente).idSede(idSede)
                        .canalReserva(request.getCanalReserva())
                        .fechaEvento(request.getFechaEvento())
                        .nombreNino(request.getNombreNino())
                        .edadNino(request.getEdadNino())
                        .nombreAcompanante(request.getNombreAcompanante())
                        .dniAcompanante(request.getDniAcompanante())
                        .firmoConsentimiento(request.getFirmoConsentimiento())
                        .idPromocionManual(request.getIdPromocionManual())
                        .medioPago(request.getMedioPago())
                        .build());

        if (query.isFirmoConsentimiento()) {
            try {
                consentimientoUseCase.registrar("RESERVA", query.getId(), List.of("ACTA"));
            } catch (Exception e) {
                log.warn("No se pudo registrar el consentimiento de la reserva {}: {}",
                        query.getId(), e.getMessage());
            }
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toResponse(query)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('reserva.crear')")
    @RateLimited(requests = 5, durationInSeconds = 60)
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> crearConParams(
            @RequestParam Long idCliente,
            @RequestParam Long idSede,
            @Valid @RequestBody CrearReservaRequest request) {
        return crear(idCliente, idSede, request);
    }

    @PostMapping("/{idReserva}/reprogramar")
    @PreAuthorize("hasAuthority('reserva.reprogramar')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> reprogramar(
            @PathVariable Long idReserva,
            @Valid @RequestBody ReprogramarReservaRequest request) {

        return ResponseEntity.ok(ApiResponse.ok(toResponse(
                reprogramarUseCase.ejecutar(ReprogramarReservaCommand.builder()
                        .idReservaOriginal(idReserva)
                        .nuevaFechaEvento(request.getNuevaFechaEvento())
                        .build()))));
    }

    @PostMapping("/{idReserva}/cancelar")
    @PreAuthorize("hasAuthority('reserva.cancelar')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> cancelar(
            @PathVariable Long idReserva,
            @RequestParam String motivo) {

        return ResponseEntity.ok(ApiResponse.ok(
                toResponse(cancelarUseCase.ejecutar(idReserva, motivo))));
    }

    @PostMapping("/{idReserva}/ingreso")
    @PreAuthorize("hasAuthority('reserva.marcar_ingreso')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> confirmarIngreso(
            @PathVariable Long idReserva) {

        return ResponseEntity.ok(ApiResponse.ok(
                toResponse(ingresoUseCase.ejecutar(idReserva,
                        supabaseAuthFacade.usuarioActualId()
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado"))))));
    }

    @PostMapping("/{id}/confirmar-pago")
    @PreAuthorize("hasAuthority('reserva.confirmar_pago')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> confirmarPago(
            @PathVariable Long id,
            @RequestParam String medioPago) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(reservaService.confirmarPago(id, medioPago))));
    }

    @PostMapping("/{id}/rechazar-pago")
    @PreAuthorize("hasAuthority('reserva.confirmar_pago')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> rechazarPago(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(reservaService.rechazarPago(id, motivo))));
    }

    @PostMapping("/{id}/reembolso-no-show")
    @PreAuthorize("hasAuthority('reserva.cancelar')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> reembolsarPorNoShow(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo) {
        return ResponseEntity.ok(ApiResponse.ok(toResponse(reservaService.reembolsarPorNoShow(id, motivo))));
    }

    @GetMapping("/{idReserva}/estado-correo")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<ApiResponse<EstadoEntregaCorreoResponse>> estadoCorreo(
            @PathVariable Long idReserva) {
        ReservaPublicaQuery reserva = reservaService.consultarPorId(idReserva);
        validarAccesoReserva(reserva);

        EstadoEntregaQuery estado = estadoEntregaUseCase.consultarPorEntidad("reserva_publica", idReserva);
        return ResponseEntity.ok(ApiResponse.ok(EstadoEntregaCorreoResponse.builder()
                .estado(estado.getEstado())
                .fechaEnvio(estado.getFechaEnvio())
                .mensajeError(estado.getMensajeError())
                .build()));
    }

    @GetMapping("/{idReserva}/ticket")
    @PreAuthorize("hasAuthority('reserva.ver')")
    public ResponseEntity<byte[]> descargarTicket(@PathVariable Long idReserva) {
        ReservaPublicaQuery reserva = reservaService.consultarPorId(idReserva);
        sedeScope.validarAcceso(reserva.getIdSede());

        String nombreSede = sedeRepository.findById(reserva.getIdSede())
                .map(s -> s.getNombre())
                .orElse("Sede Principal");

        byte[] pdf = ticketIngresoPdfService.generarTicketPdf(reserva, nombreSede);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "ticket-" + reserva.getNumeroTicket() + ".pdf");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    @PostMapping("/{idReserva}/comprobante")
    @PreAuthorize("hasAuthority('reserva.editar')")
    public ResponseEntity<ApiResponse<ReservaPublicaResponse>> subirComprobante(
            @PathVariable Long idReserva,
            @RequestParam("archivo") MultipartFile archivo) {

        String tipo = archivo.getContentType();
        if (tipo == null || (!tipo.startsWith("image/") && !tipo.equals("application/pdf"))) {
            throw new ValidationException("El archivo debe ser una imagen o PDF.");
        }
        if (archivo.getSize() > 10L * 1024 * 1024) {
            throw new ValidationException("El archivo no puede superar 10 MB.");
        }
        return ResponseEntity.ok(ApiResponse.ok(
                toResponse(reservaService.actualizarReferenciaPago(idReserva, archivo))));
    }

    @GetMapping("/control-acceso/ticket/{numeroTicket}")
    @PreAuthorize("hasAuthority('reserva.marcar_ingreso')")
    public ResponseEntity<ApiResponse<TicketDetalleResponse>> buscarTicketDetalle(
            @PathVariable String numeroTicket) {
        TicketDetalleQuery q = reservaAdminService.buscarTicketDetalle(numeroTicket);
        return ResponseEntity.ok(ApiResponse.ok(toDetalleResponse(q)));
    }

    @PostMapping("/control-acceso/{idReserva}/marcar-entrada")
    @PreAuthorize("hasAuthority('reserva.marcar_ingreso')")
    public ResponseEntity<ApiResponse<TicketDetalleResponse>> marcarEntrada(
            @PathVariable Long idReserva) {
        TicketDetalleQuery q = reservaAdminService.marcarEntrada(idReserva);
        return ResponseEntity.ok(ApiResponse.ok(toDetalleResponse(q)));
    }

    @PatchMapping("/control-acceso/{idReserva}/fecha")
    @PreAuthorize("hasAuthority('reserva.editar')")
    public ResponseEntity<ApiResponse<TicketDetalleResponse>> editarFecha(
            @PathVariable Long idReserva,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nuevaFecha) {
        TicketDetalleQuery q = reservaAdminService.editarFecha(idReserva, nuevaFecha);
        return ResponseEntity.ok(ApiResponse.ok(toDetalleResponse(q)));
    }

    @PostMapping("/control-acceso/{idReserva}/revertir-ingreso")
    @PreAuthorize("hasAuthority('reserva.editar')")
    public ResponseEntity<ApiResponse<TicketDetalleResponse>> revertirIngreso(
            @PathVariable Long idReserva) {
        TicketDetalleQuery q = reservaAdminService.revertirIngreso(idReserva);
        return ResponseEntity.ok(ApiResponse.ok(toDetalleResponse(q)));
    }

    @PostMapping("/control-acceso/{idReserva}/salida")
    @PreAuthorize("hasAuthority('reserva.marcar_ingreso')")
    public ResponseEntity<ApiResponse<TicketDetalleResponse>> registrarSalida(
            @PathVariable Long idReserva) {
        TicketDetalleQuery q = reservaAdminService.registrarSalida(idReserva);
        return ResponseEntity.ok(ApiResponse.ok(toDetalleResponse(q)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('reserva.cancelar')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> eliminar(@PathVariable Long id) {
        reservaAdminService.eliminar(id);
        return ApiResponse.noContent();
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

    private void validarAccesoReserva(ReservaPublicaQuery reserva) {
        if (supabaseAuthFacade.tieneRol("CLIENTE")) {
            Long propio = supabaseAuthFacade.clientePerfilId()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "Cliente sin perfil asociado"));
            if (!propio.equals(reserva.getIdCliente())) {
                throw new AccessDeniedException("No puedes consultar datos de otro cliente");
            }
            return;
        }
        sedeScope.validarAcceso(reserva.getIdSede());
    }

    private TicketDetalleResponse toDetalleResponse(TicketDetalleQuery q) {
        return TicketDetalleResponse.builder()
                .idReserva(q.getIdReserva())
                .numeroTicket(q.getNumeroTicket())
                .estado(q.getEstado())
                .yaIngreso(q.isYaIngreso())
                .fechaIngreso(q.getFechaIngreso())
                .permanenciaVigente(q.isPermanenciaVigente())
                .permanenciaFinAt(q.getPermanenciaFinAt())
                .fechaVisita(q.getFechaVisita())
                .esHoy(q.isEsHoy())
                .nombreNino(q.getNombreNino())
                .edadNino(q.getEdadNino())
                .nombreAcompanante(q.getNombreAcompanante())
                .dniAcompanante(q.getDniAcompanante())
                .montoPagado(q.getMontoPagado())
                .estadoPago(q.getEstadoPago())
                .codigoQr(q.getCodigoQr())
                .build();
    }

    private ReservaPublicaResponse toResponse(ReservaPublicaQuery q) {
        return ReservaPublicaResponse.builder()
                .id(q.getId())
                .idCliente(q.getIdCliente())
                .nombreCliente(q.getNombreCliente())
                .correoCliente(q.getCorreoCliente())
                .idSede(q.getIdSede())
                .nombreSede(q.getNombreSede())
                .estado(q.getEstado())
                .canalReserva(q.getCanalReserva())
                .tipoDia(q.getTipoDia())
                .fechaEvento(q.getFechaEvento())
                .numeroTicket(q.getNumeroTicket())
                .precioHistorico(q.getPrecioHistorico())
                .descuentoAplicado(q.getDescuentoAplicado())
                .totalPagado(q.getTotalPagado())
                .nombreNino(q.getNombreNino())
                .edadNino(q.getEdadNino())
                .nombreAcompanante(q.getNombreAcompanante())
                .dniAcompanante(q.getDniAcompanante())
                .firmoConsentimiento(q.isFirmoConsentimiento())
                .esReprogramacion(q.isEsReprogramacion())
                .vecesReprogramada(q.getVecesReprogramada())
                .ingresado(q.isIngresado())
                .fechaIngreso(q.getFechaIngreso())
                .duracionHistoricaMinutos(q.getDuracionHistoricaMinutos())
                .permanenciaFinAt(q.getPermanenciaFinAt())
                .codigoQr(q.getCodigoQr())
                .medioPago(q.getMedioPago())
                .referenciaPago(q.getReferenciaPago())
                .motivoRechazoPago(q.getMotivoRechazoPago())
                .motivoCancelacion(q.getMotivoCancelacion())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }
}
