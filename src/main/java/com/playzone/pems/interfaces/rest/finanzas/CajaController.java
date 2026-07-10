package com.playzone.pems.interfaces.rest.finanzas;

import com.playzone.pems.application.finanzas.dto.command.AbrirCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.AnularMovimientoCommand;
import com.playzone.pems.application.finanzas.dto.command.CerrarCajaCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarArqueoCommand;
import com.playzone.pems.application.finanzas.dto.command.RegistrarMovimientoManualCommand;
import com.playzone.pems.application.finanzas.dto.query.ArqueoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.MovimientoCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.ResumenCajaQuery;
import com.playzone.pems.application.finanzas.dto.query.SesionCajaQuery;
import com.playzone.pems.application.finanzas.port.in.GestionarCajaUseCase;
import com.playzone.pems.domain.finanzas.model.enums.TipoSesionCaja;
import com.playzone.pems.infrastructure.security.SedeScopeValidator;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.finanzas.request.AbrirCajaRequest;
import com.playzone.pems.interfaces.rest.finanzas.request.AnularMovimientoRequest;
import com.playzone.pems.interfaces.rest.finanzas.request.CerrarCajaForzadoRequest;
import com.playzone.pems.interfaces.rest.finanzas.request.CerrarCajaRequest;
import com.playzone.pems.interfaces.rest.finanzas.request.RegistrarArqueoRequest;
import com.playzone.pems.interfaces.rest.finanzas.request.RegistrarMovimientoManualRequest;
import com.playzone.pems.interfaces.rest.finanzas.response.ArqueoCajaResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.MovimientoCajaResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.ResumenCajaResponse;
import com.playzone.pems.interfaces.rest.finanzas.response.SesionCajaResponse;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/caja")
@RequiredArgsConstructor
public class CajaController {

    private final GestionarCajaUseCase useCase;
    private final SupabaseAuthFacade   supabaseAuthFacade;
    private final SedeScopeValidator   sedeScope;

    @PostMapping("/sedes/{idSede}/abrir")
    @PreAuthorize("hasAuthority('caja.abrir')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> abrir(
            @PathVariable Long idSede,
            @Valid @RequestBody AbrirCajaRequest request) {
        sedeScope.validarAcceso(idSede);
        UUID usuario = usuarioActual();
        SesionCajaQuery query = useCase.abrir(AbrirCajaCommand.builder()
                .idSede(idSede)
                .tipo(tipoSesionDelUsuario())
                .saldoInicial(request.getSaldoInicial())
                .idUsuarioApertura(usuario)
                .observaciones(request.getObservaciones())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toResponse(query)));
    }

    @PutMapping("/{idSesion}/cerrar")
    @PreAuthorize("hasAuthority('caja.cerrar')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> cerrar(
            @PathVariable Long idSesion,
            @Valid @RequestBody CerrarCajaRequest request) {
        SesionCajaQuery query = useCase.cerrar(CerrarCajaCommand.builder()
                .idSesionCaja(idSesion)
                .saldoFinal(request.getSaldoFinal())
                .idUsuarioCierre(usuarioActual())
                .esAdmin(esAdmin())
                .observaciones(request.getObservaciones())
                .build());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(query)));
    }

    @PutMapping("/{idSesion}/cerrar-forzado")
    @PreAuthorize("hasAuthority('caja.cerrar')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> cerrarForzado(
            @PathVariable Long idSesion,
            @Valid @RequestBody CerrarCajaForzadoRequest request) {
        if (!esAdmin()) {
            throw new ValidationException("Solo un administrador puede realizar un cierre forzado.");
        }
        SesionCajaQuery query = useCase.cerrar(CerrarCajaCommand.builder()
                .idSesionCaja(idSesion)
                .saldoFinal(request.getSaldoFinal())
                .idUsuarioCierre(usuarioActual())
                .esAdmin(true)
                .motivo(request.getMotivo())
                .observaciones(request.getObservaciones())
                .build());
        return ResponseEntity.ok(ApiResponse.ok(toResponse(query)));
    }

    @GetMapping("/mi-sesion")
    @PreAuthorize("hasAuthority('caja.ver_historial') or hasAuthority('caja.abrir')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> miSesion() {
        return ResponseEntity.ok(ApiResponse.ok(
                useCase.obtenerMiSesion(usuarioActual()).map(this::toResponse).orElse(null)));
    }

    @GetMapping("/sedes/{idSede}/mi-sesion")
    @PreAuthorize("hasAuthority('caja.ver_historial') or hasAuthority('caja.abrir')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> miSesionEnSede(@PathVariable Long idSede) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(
                useCase.obtenerMiSesionEnSede(usuarioActual(), idSede).map(this::toResponse).orElse(null)));
    }

    @GetMapping("/sedes/{idSede}/fecha/{fecha}")
    @PreAuthorize("hasAuthority('caja.ver_historial') or hasAuthority('caja.abrir')")
    public ResponseEntity<ApiResponse<SesionCajaResponse>> miSesionPorFecha(
            @PathVariable Long idSede,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        sedeScope.validarAcceso(idSede);
        return ResponseEntity.ok(ApiResponse.ok(
                useCase.obtenerMiSesionPorFecha(usuarioActual(), idSede, fecha)
                        .map(this::toResponse).orElse(null)));
    }

    @GetMapping("/sedes/{idSede}/rango")
    @PreAuthorize("hasAuthority('caja.ver_historial')")
    public ResponseEntity<ApiResponse<List<SesionCajaResponse>>> listarPorRango(
            @PathVariable Long idSede,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        sedeScope.validarAcceso(idSede);
        List<SesionCajaResponse> body = useCase.listarPorRango(idSede, inicio, fin)
                .stream().map(this::toResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/{idSesion}/movimientos")
    @PreAuthorize("hasAuthority('caja.ver_historial')")
    public ResponseEntity<ApiResponse<List<MovimientoCajaResponse>>> listarMovimientos(
            @PathVariable Long idSesion) {
        List<MovimientoCajaResponse> body = useCase.listarMovimientos(idSesion)
                .stream().map(this::toMovimientoResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @PostMapping("/{idSesion}/movimientos")
    @PreAuthorize("hasAuthority('caja.movimiento')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> registrarMovimiento(
            @PathVariable Long idSesion,
            @Valid @RequestBody RegistrarMovimientoManualRequest request) {
        MovimientoCajaQuery query = useCase.registrarMovimiento(RegistrarMovimientoManualCommand.builder()
                .idSesionCaja(idSesion)
                .tipo(request.getTipo())
                .concepto(request.getConcepto())
                .monto(request.getMonto())
                .medioPago(request.getMedioPago())
                .categoriaRetiro(request.getCategoriaRetiro())
                .idUsuarioRegistra(usuarioActual())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toMovimientoResponse(query)));
    }

    @PostMapping("/movimientos/{idMovimiento}/anular")
    @PreAuthorize("hasAuthority('caja.movimiento')")
    public ResponseEntity<ApiResponse<MovimientoCajaResponse>> anularMovimiento(
            @PathVariable Long idMovimiento,
            @Valid @RequestBody AnularMovimientoRequest request) {
        MovimientoCajaQuery query = useCase.anularMovimiento(AnularMovimientoCommand.builder()
                .idMovimiento(idMovimiento)
                .motivo(request.getMotivo())
                .idUsuarioAnula(usuarioActual())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toMovimientoResponse(query)));
    }

    @PostMapping("/{idSesion}/arqueos")
    @PreAuthorize("hasAuthority('caja.cerrar') or hasAuthority('caja.movimiento')")
    public ResponseEntity<ApiResponse<ArqueoCajaResponse>> registrarArqueo(
            @PathVariable Long idSesion,
            @Valid @RequestBody RegistrarArqueoRequest request) {
        ArqueoCajaQuery query = useCase.registrarArqueo(RegistrarArqueoCommand.builder()
                .idSesionCaja(idSesion)
                .saldoContado(request.getSaldoContado())
                .observaciones(request.getObservaciones())
                .realizadoPor(usuarioActual())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(toArqueoResponse(query)));
    }

    @GetMapping("/{idSesion}/arqueos")
    @PreAuthorize("hasAuthority('caja.ver_historial')")
    public ResponseEntity<ApiResponse<List<ArqueoCajaResponse>>> listarArqueos(
            @PathVariable Long idSesion) {
        List<ArqueoCajaResponse> body = useCase.listarArqueos(idSesion)
                .stream().map(this::toArqueoResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(body));
    }

    @GetMapping("/{idSesion}/resumen")
    @PreAuthorize("hasAuthority('caja.ver_historial') or hasAuthority('caja.cerrar')")
    public ResponseEntity<ApiResponse<ResumenCajaResponse>> resumen(@PathVariable Long idSesion) {
        ResumenCajaQuery query = useCase.generarResumen(idSesion);
        return ResponseEntity.ok(ApiResponse.ok(toResumenResponse(query)));
    }

    private UUID usuarioActual() {
        return supabaseAuthFacade.usuarioActualId()
                .orElseThrow(() -> new ValidationException("Sesion de usuario requerida."));
    }

    private boolean esAdmin() {
        return supabaseAuthFacade.tieneRol("SUPERADMIN") || supabaseAuthFacade.tieneRol("ADMIN");
    }

    private TipoSesionCaja tipoSesionDelUsuario() {
        return esAdmin() ? TipoSesionCaja.ADMINISTRATIVA : TipoSesionCaja.CAJERO;
    }

    private SesionCajaResponse toResponse(SesionCajaQuery q) {
        return SesionCajaResponse.builder()
                .id(q.getId())
                .idSede(q.getIdSede())
                .usuarioId(q.getUsuarioId())
                .tipo(q.getTipo())
                .fecha(q.getFecha())
                .saldoInicial(q.getSaldoInicial())
                .saldoFinal(q.getSaldoFinal())
                .totalIngresos(q.getTotalIngresos())
                .totalEgresos(q.getTotalEgresos())
                .saldoEsperado(q.getSaldoEsperado())
                .diferencia(q.getDiferencia())
                .estado(q.getEstado())
                .cerradaPor(q.getCerradaPor())
                .motivoCierre(q.getMotivoCierre())
                .fechaApertura(q.getFechaApertura())
                .fechaCierre(q.getFechaCierre())
                .observaciones(q.getObservaciones())
                .build();
    }

    private MovimientoCajaResponse toMovimientoResponse(MovimientoCajaQuery q) {
        return MovimientoCajaResponse.builder()
                .id(q.getId())
                .idSesionCaja(q.getIdSesionCaja())
                .tipo(q.getTipo())
                .concepto(q.getConcepto())
                .monto(q.getMonto())
                .medioPago(q.getMedioPago())
                .categoriaRetiro(q.getCategoriaRetiro())
                .idRegistroIngreso(q.getIdRegistroIngreso())
                .idRegistroEgreso(q.getIdRegistroEgreso())
                .idVenta(q.getIdVenta())
                .esManual(q.isEsManual())
                .naturaleza(q.getNaturaleza())
                .idMovimientoAnulado(q.getIdMovimientoAnulado())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }

    private ArqueoCajaResponse toArqueoResponse(ArqueoCajaQuery q) {
        return ArqueoCajaResponse.builder()
                .id(q.getId())
                .idSesionCaja(q.getIdSesionCaja())
                .saldoEsperado(q.getSaldoEsperado())
                .saldoContado(q.getSaldoContado())
                .diferencia(q.getDiferencia())
                .observaciones(q.getObservaciones())
                .realizadoPor(q.getRealizadoPor())
                .fechaCreacion(q.getFechaCreacion())
                .build();
    }

    private ResumenCajaResponse toResumenResponse(ResumenCajaQuery q) {
        return ResumenCajaResponse.builder()
                .id(q.getId())
                .idSede(q.getIdSede())
                .usuarioId(q.getUsuarioId())
                .tipo(q.getTipo())
                .fecha(q.getFecha())
                .saldoInicial(q.getSaldoInicial())
                .totalIngresos(q.getTotalIngresos())
                .totalEgresos(q.getTotalEgresos())
                .saldoEsperado(q.getSaldoEsperado())
                .saldoFinal(q.getSaldoFinal())
                .diferencia(q.getDiferencia())
                .estado(q.getEstado())
                .fechaApertura(q.getFechaApertura())
                .fechaCierre(q.getFechaCierre())
                .observaciones(q.getObservaciones())
                .movimientos(q.getMovimientos() != null
                        ? q.getMovimientos().stream().map(this::toMovimientoResponse).toList() : null)
                .arqueos(q.getArqueos() != null
                        ? q.getArqueos().stream().map(this::toArqueoResponse).toList() : null)
                .build();
    }
}
