package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.application.usuario.port.in.ActivarCuentaStaffUseCase;
import com.playzone.pems.application.usuario.port.in.LoginUseCase;
import com.playzone.pems.application.usuario.port.in.RecuperarPasswordUseCase;
import com.playzone.pems.interfaces.rest.usuario.request.ActivarCuentaStaffRequest;
import com.playzone.pems.interfaces.rest.usuario.request.LoginRequest;
import com.playzone.pems.interfaces.rest.usuario.request.RecuperarPasswordRequest;
import com.playzone.pems.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase             loginUseCase;
    private final RecuperarPasswordUseCase recuperarPasswordUseCase;
    private final ActivarCuentaStaffUseCase activarCuentaStaffUseCase;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        String ip = Optional.ofNullable(servletRequest.getHeader("X-Forwarded-For"))
                .map(h -> h.split(",")[0].trim())
                .orElse(servletRequest.getRemoteAddr());
        String ua = servletRequest.getHeader("User-Agent");
        return ResponseEntity.ok(ApiResponse.ok(loginUseCase.ejecutar(request.getEmail(), request.getPassword(), ip, ua)));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<ApiResponse<Void>> recuperar(@Valid @RequestBody RecuperarPasswordRequest request) {
        recuperarPasswordUseCase.ejecutar(request.getEmail());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    @PostMapping("/activar-cuenta")
    public ResponseEntity<ApiResponse<Void>> activarCuenta(@Valid @RequestBody ActivarCuentaStaffRequest request) {
        activarCuentaStaffUseCase.activarCuenta(request.getToken(), request.getNuevaContrasena());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
