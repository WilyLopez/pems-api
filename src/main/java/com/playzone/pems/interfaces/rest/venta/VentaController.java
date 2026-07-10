package com.playzone.pems.interfaces.rest.venta;

import com.playzone.pems.application.evento.service.ReservaPublicaService;
import com.playzone.pems.application.venta.dto.command.CobrarReservaCommand;
import com.playzone.pems.application.venta.dto.command.PagoMostradorCommand;
import com.playzone.pems.application.venta.dto.query.VentaQuery;
import com.playzone.pems.application.venta.port.in.ConsultarVentasUseCase;
import com.playzone.pems.application.venta.port.in.ProcesarVentaUseCase;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.infrastructure.pdf.NotaVentaPdfService;
import com.playzone.pems.application.venta.dto.query.VentaDetalleQuery;
import com.playzone.pems.application.evento.dto.query.ReservaPublicaQuery;
import com.playzone.pems.interfaces.rest.evento.response.ReservaPublicaResponse;
import com.playzone.pems.interfaces.rest.venta.request.CobrarReservaRequest;
import com.playzone.pems.interfaces.rest.venta.response.VentaResponse;
import com.playzone.pems.interfaces.rest.venta.response.VentaDetalleResponse;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final ProcesarVentaUseCase   procesarUseCase;
    private final ConsultarVentasUseCase consultarUseCase;
    private final SupabaseAuthFacade     supabaseAuthFacade;
    private final NotaVentaPdfService    notaVentaPdfService;
    private final SedeScopeValidator     sedeScope;
    private final ReservaPublicaService  reservaPublicaService;

    @PostMapping("/reserva/{reservaId}/cobrar")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<VentaResponse>> cobrarReserva(
            @PathVariable Long reservaId,
            @Valid @RequestBody CobrarReservaRequest request) {

        sedeScope.validarAcceso(reservaPublicaService.consultarPorId(reservaId).getIdSede());

        VentaQuery query = procesarUseCase.cobrarReserva(CobrarReservaCommand.builder()
                .reservaId(reservaId)
                .efectivoRecibido(request.getEfectivoRecibido())
                .actaFirmada(request.isActaFirmada())
                .notas(request.getNotas())
                .pagos(request.getPagos().stream()
                        .map(p -> PagoMostradorCommand.builder()
                                .medioPago(p.getMedioPago())
                                .monto(p.getMonto())
                                .referencia(p.getReferencia())
                                .build())
                        .toList())
                .createdBy(supabaseAuthFacade.usuarioActualId()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado")))
                .build());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(toResponse(query)));
    }

    @GetMapping("/sedes/{idSede}")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<Page<VentaResponse>>> listar(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        sedeScope.validarAcceso(idSede);
        Page<VentaResponse> page = consultarUseCase.consultarPorSedeYFechas(idSede, desde, hasta, search, pageable)
                .map(this::toResponse);
        return ResponseEntity.ok(ApiResponse.ok(page));
    }

    @GetMapping("/{idVenta}")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<VentaResponse>> consultar(@PathVariable Long idVenta) {
        VentaQuery query = consultarUseCase.consultarPorId(idVenta);
        sedeScope.validarAcceso(query.getIdSede());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(query)));
    }

    @GetMapping("/{idVenta}/detalle")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<VentaDetalleResponse>> consultarDetalle(@PathVariable Long idVenta) {
        VentaDetalleQuery detalle = consultarUseCase.consultarDetallePorId(idVenta);
        sedeScope.validarAcceso(detalle.getIdSede());
        return ResponseEntity.ok(ApiResponse.ok(toDetalleResponse(detalle)));
    }

    @PostMapping("/{idVenta}/enviar-correo")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<Void>> enviarCorreo(
            @PathVariable Long idVenta,
            @RequestParam(required = false) String correo) {
        sedeScope.validarAcceso(consultarUseCase.consultarPorId(idVenta).getIdSede());
        procesarUseCase.enviarCorreoVenta(idVenta, correo);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{idVenta}/marcar-impreso")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<Void>> marcarImpreso(@PathVariable Long idVenta) {
        sedeScope.validarAcceso(consultarUseCase.consultarPorId(idVenta).getIdSede());
        procesarUseCase.marcarImpreso(idVenta);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{idVenta}/marcar-descargado")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<ApiResponse<Void>> marcarDescargado(@PathVariable Long idVenta) {
        sedeScope.validarAcceso(consultarUseCase.consultarPorId(idVenta).getIdSede());
        procesarUseCase.marcarDescargado(idVenta);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{idVenta}/nota-venta")
    @PreAuthorize("hasAuthority('pos.vender')")
    public ResponseEntity<byte[]> descargarNotaVenta(@PathVariable Long idVenta) {
        VentaQuery venta = consultarUseCase.consultarPorId(idVenta);
        sedeScope.validarAcceso(venta.getIdSede());
        byte[] pdf = notaVentaPdfService.generarNotaVentaPdf(venta, "Sede del Negocio");

        procesarUseCase.marcarDescargado(idVenta);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "nota-venta-" + venta.getId() + ".pdf");

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    private VentaResponse toResponse(VentaQuery q) {
        return VentaResponse.builder()
                .id(q.getId())
                .idSede(q.getIdSede())
                .clienteId(q.getClienteId())
                .eventoId(q.getEventoId())
                .tipo(q.getTipo())
                .canalCodigo(q.getCanalCodigo())
                .fechaVisita(q.getFechaVisita())
                .subtotal(q.getSubtotal())
                .descuento(q.getDescuento())
                .total(q.getTotal())
                .nombreAcompanante(q.getNombreAcompanante())
                .dniAcompanante(q.getDniAcompanante())
                .nombreCliente(q.getNombreCliente())
                .notas(q.getNotas())
                .impreso(q.isImpreso())
                .enviadoCorreo(q.isEnviadoCorreo())
                .descargado(q.isDescargado())
                .efectivoRecibido(q.getEfectivoRecibido())
                .vuelto(q.getVuelto())
                .createdAt(q.getCreatedAt())
                .build();
    }

    private VentaDetalleResponse toDetalleResponse(VentaDetalleQuery q) {
        return VentaDetalleResponse.builder()
                .id(q.getId())
                .idSede(q.getIdSede())
                .clienteId(q.getClienteId())
                .eventoId(q.getEventoId())
                .tipo(q.getTipo())
                .canalCodigo(q.getCanalCodigo())
                .fechaVisita(q.getFechaVisita())
                .subtotal(q.getSubtotal())
                .descuento(q.getDescuento())
                .total(q.getTotal())
                .nombreAcompanante(q.getNombreAcompanante())
                .dniAcompanante(q.getDniAcompanante())
                .telefonoAcompanante(q.getTelefonoAcompanante())
                .nombreCliente(q.getNombreCliente())
                .notas(q.getNotas())
                .impreso(q.isImpreso())
                .enviadoCorreo(q.isEnviadoCorreo())
                .descargado(q.isDescargado())
                .efectivoRecibido(q.getEfectivoRecibido())
                .vuelto(q.getVuelto())
                .createdAt(q.getCreatedAt())
                .tickets(q.getTickets().stream().map(this::toReservaResponse).toList())
                .pagos(q.getPagos().stream().map(p -> VentaDetalleResponse.PagoDetalleResponse.builder()
                        .id(p.getId())
                        .medioPago(p.getMedioPago())
                        .monto(p.getMonto())
                        .referencia(p.getReferencia())
                        .esValidado(p.isEsValidado())
                        .build()).toList())
                .totalPagado(q.getTotalPagado())
                .build();
    }

    private ReservaPublicaResponse toReservaResponse(ReservaPublicaQuery q) {
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
                .codigoQr(q.getCodigoQr())
                .medioPago(q.getMedioPago())
                .referenciaPago(q.getReferenciaPago())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }
}
