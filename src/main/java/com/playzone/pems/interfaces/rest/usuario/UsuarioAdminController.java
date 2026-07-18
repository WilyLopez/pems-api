package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.application.usuario.dto.command.RegistrarUsuarioAdminCommand;
import com.playzone.pems.application.usuario.dto.response.UsuarioAdminResponse;
import com.playzone.pems.application.usuario.port.in.ActivarUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.ActualizarUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.CambiarRolUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.DesactivarUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.DesbloquearUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.ListarUsuariosAdminUseCase;
import com.playzone.pems.application.usuario.port.in.ObtenerUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.RegistrarUsuarioAdminUseCase;
import com.playzone.pems.application.usuario.port.in.ResetPasswordAdminUseCase;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.interfaces.rest.usuario.request.ActualizarUsuarioAdminRequest;
import com.playzone.pems.interfaces.rest.usuario.request.CambiarRolRequest;
import com.playzone.pems.interfaces.rest.usuario.request.RegistrarUsuarioAdminRequest;
import com.playzone.pems.shared.exception.ValidationException;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios-admin")
@RequiredArgsConstructor
public class UsuarioAdminController {

    private final ListarUsuariosAdminUseCase     listarUseCase;
    private final ObtenerUsuarioAdminUseCase     obtenerUseCase;
    private final RegistrarUsuarioAdminUseCase   registrarUseCase;
    private final ActualizarUsuarioAdminUseCase  actualizarUseCase;
    private final CambiarRolUsuarioAdminUseCase  cambiarRolUseCase;
    private final ResetPasswordAdminUseCase      resetPasswordUseCase;
    private final ActivarUsuarioAdminUseCase     activarUseCase;
    private final DesactivarUsuarioAdminUseCase  desactivarUseCase;
    private final DesbloquearUsuarioAdminUseCase desbloquearUseCase;
    private final SupabaseAuthFacade             authFacade;

    @GetMapping
    @PreAuthorize("hasAuthority('usuarios.ver')")
    public ResponseEntity<ApiResponse<List<UsuarioAdminResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(listarUseCase.ejecutar()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('usuarios.ver') or #id == @supabaseAuthFacade.staffPerfilId().orElse(-1L)")
    public ResponseEntity<ApiResponse<UsuarioAdminResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(obtenerUseCase.ejecutar(id)));
    }

    @PostMapping("/sedes/{sedeId}")
    @PreAuthorize("hasAuthority('usuarios.crear')")
    public ResponseEntity<ApiResponse<UsuarioAdminResponse>> registrar(
            @PathVariable Long sedeId,
            @Valid @RequestBody RegistrarUsuarioAdminRequest request) {

        UsuarioAdminResponse response = registrarUseCase.ejecutar(
                RegistrarUsuarioAdminCommand.builder()
                        .nombre(request.getNombre())
                        .correo(request.getCorreo())
                        .rolCodigo(request.getRol())
                        .sedeId(sedeId)
                        .telefono(request.getTelefono())
                        .build()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('usuarios.editar') or #id == @supabaseAuthFacade.staffPerfilId().orElse(-1L)")
    public ResponseEntity<ApiResponse<UsuarioAdminResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarUsuarioAdminRequest request) {

        UsuarioAdminResponse response = actualizarUseCase.ejecutar(id, request.getNombre(), request.getTelefono());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    public ResponseEntity<ApiResponse<UsuarioAdminResponse>> cambiarRol(
            @PathVariable Long id,
            @Valid @RequestBody CambiarRolRequest request) {

        UUID solicitanteId = authFacade.usuarioActualId()
                .orElseThrow(() -> new ValidationException("auth", "No se pudo identificar al solicitante."));

        UsuarioAdminResponse response = cambiarRolUseCase.ejecutar(id, request.getNuevoRol(), solicitanteId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@PathVariable Long id) {
        resetPasswordUseCase.resetear(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    public ResponseEntity<ApiResponse<Void>> activar(@PathVariable Long id) {
        activarUseCase.activar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    public ResponseEntity<ApiResponse<Void>> desactivar(@PathVariable Long id) {
        desactivarUseCase.desactivar(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('usuarios.editar')")
    public ResponseEntity<ApiResponse<Void>> desbloquear(@PathVariable Long id) {
        desbloquearUseCase.desbloquear(id);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
