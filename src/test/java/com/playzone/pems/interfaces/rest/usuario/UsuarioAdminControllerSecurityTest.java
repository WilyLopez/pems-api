package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.application.usuario.dto.response.UsuarioAdminResponse;
import com.playzone.pems.application.usuario.port.in.*;
import com.playzone.pems.infrastructure.security.SecurityConfig;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.infrastructure.security.SupabaseJwtFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de Seguridad — UsuarioAdminController
 *
 * Categorías:
 *   1. Acceso sin autenticación          → HTTP 401
 *   2. Acceso con permisos insuficientes → HTTP 403 (IDOR: Fase 1 del plan de implementación)
 *   3. Acceso correcto con permisos válidos → HTTP 2xx
 *   4. Autoservicio "Mi Perfil" — un staff sin permiso administrativo accede a su propio registro
 *
 * SupabaseJwtFilter se reemplaza con @MockBean porque su lógica real depende de un JWKS de
 * Supabase inalcanzable en test. Al ser un mock, su doFilterInternal() no invoca la cadena por
 * defecto, así que se stubea para delegar directamente al FilterChain y no interrumpir el resto
 * del pipeline de seguridad (el estado de autenticación lo inyecta TestSecurityUtils).
 */
@WebMvcTest(UsuarioAdminController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("Seguridad - UsuarioAdminController")
class UsuarioAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private ListarUsuariosAdminUseCase     listarUseCase;
    @MockBean private ObtenerUsuarioAdminUseCase     obtenerUseCase;
    @MockBean private RegistrarUsuarioAdminUseCase   registrarUseCase;
    @MockBean private ActualizarUsuarioAdminUseCase  actualizarUseCase;
    @MockBean private CambiarRolUsuarioAdminUseCase  cambiarRolUseCase;
    @MockBean private ResetPasswordAdminUseCase      resetPasswordUseCase;
    @MockBean private ActivarUsuarioAdminUseCase     activarUseCase;
    @MockBean private DesactivarUsuarioAdminUseCase  desactivarUseCase;
    @MockBean private DesbloquearUsuarioAdminUseCase desbloquearUseCase;
    @MockBean(name = "supabaseAuthFacade") private SupabaseAuthFacade supabaseAuthFacade;
    @MockBean private SupabaseJwtFilter              supabaseJwtFilter;

    private static final Long STAFF_ID = 7L;
    private static final UUID CLIENTE_UUID = UUID.randomUUID();
    private static final UUID ADMIN_UUID = UUID.randomUUID();
    private static final UUID CAJERO_UUID = UUID.randomUUID();

    @BeforeEach
    void permitirPasoDelFiltroMockeado() throws Exception {
        doAnswer(invocation -> {
            ServletRequest request = invocation.getArgument(0);
            ServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(supabaseJwtFilter).doFilter(any(), any(), any());
    }

    @AfterEach
    void limpiarContexto() {
        TestSecurityUtils.clearAuthentication();
    }

    /* ═══════════════════════════════════════════════════════════════
       1. Sin autenticación → 401
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /api/v1/usuarios-admin/{id} sin token → 401")
    void obtener_sinAutenticacion_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios-admin/" + STAFF_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios-admin/{id} sin token → 401")
    void actualizar_sinAutenticacion_returns401() throws Exception {
        mockMvc.perform(put("/api/v1/usuarios-admin/" + STAFF_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content(cuerpoActualizar()))
                .andExpect(status().isUnauthorized());
    }

    /* ═══════════════════════════════════════════════════════════════
       2. IDOR — autenticado pero sin el permiso correspondiente → 403
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("IDOR: GET /api/v1/usuarios-admin/{id} como cliente autenticado → 403")
    void obtener_sinPermisoVer_returns403() throws Exception {
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, 99L, List.of("cliente.ver"));

        mockMvc.perform(get("/api/v1/usuarios-admin/" + STAFF_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IDOR: PUT /api/v1/usuarios-admin/{id} como cliente autenticado → 403")
    void actualizar_sinPermisoEditar_returns403() throws Exception {
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, 99L, List.of("cliente.ver"));

        mockMvc.perform(put("/api/v1/usuarios-admin/" + STAFF_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content(cuerpoActualizar()))
                .andExpect(status().isForbidden());
    }

    /* ═══════════════════════════════════════════════════════════════
       3. Acceso exitoso con permisos correctos → 2xx
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /api/v1/usuarios-admin/{id} con 'usuarios.ver' → 200")
    void obtener_conPermisoVer_returns200() throws Exception {
        TestSecurityUtils.authenticateAsAdmin(ADMIN_UUID, List.of("usuarios.ver"));
        when(obtenerUseCase.ejecutar(anyLong()))
                .thenReturn(UsuarioAdminResponse.builder().id(STAFF_ID).build());

        mockMvc.perform(get("/api/v1/usuarios-admin/" + STAFF_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios-admin/{id} con 'usuarios.editar' → 200")
    void actualizar_conPermisoEditar_returns200() throws Exception {
        TestSecurityUtils.authenticateAsAdmin(ADMIN_UUID, List.of("usuarios.editar"));
        when(actualizarUseCase.ejecutar(anyLong(), anyString(), anyString()))
                .thenReturn(UsuarioAdminResponse.builder().id(STAFF_ID).build());

        mockMvc.perform(put("/api/v1/usuarios-admin/" + STAFF_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content(cuerpoActualizar()))
                .andExpect(status().isOk());
    }

    /* ═══════════════════════════════════════════════════════════════
       4. Autoservicio "Mi Perfil" — un staff sin 'usuarios.ver'/'usuarios.editar'
          debe seguir pudiendo ver y editar su propio registro
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /api/v1/usuarios-admin/{id} propio, sin 'usuarios.ver' → 200")
    void obtener_propioSinPermisoVer_returns200() throws Exception {
        TestSecurityUtils.authenticateAsStaff(CAJERO_UUID, STAFF_ID, List.of("pos.vender"));
        when(supabaseAuthFacade.staffPerfilId()).thenReturn(java.util.Optional.of(STAFF_ID));
        when(obtenerUseCase.ejecutar(STAFF_ID))
                .thenReturn(UsuarioAdminResponse.builder().id(STAFF_ID).build());

        mockMvc.perform(get("/api/v1/usuarios-admin/" + STAFF_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios-admin/{id} propio, sin 'usuarios.editar' → 200")
    void actualizar_propioSinPermisoEditar_returns200() throws Exception {
        TestSecurityUtils.authenticateAsStaff(CAJERO_UUID, STAFF_ID, List.of("pos.vender"));
        when(supabaseAuthFacade.staffPerfilId()).thenReturn(java.util.Optional.of(STAFF_ID));
        when(actualizarUseCase.ejecutar(STAFF_ID, "Juan Perez", "999999999"))
                .thenReturn(UsuarioAdminResponse.builder().id(STAFF_ID).build());

        mockMvc.perform(put("/api/v1/usuarios-admin/" + STAFF_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content(cuerpoActualizar()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("IDOR: GET /api/v1/usuarios-admin/{id} de otro staff, sin 'usuarios.ver' → 403")
    void obtener_otroStaffSinPermisoVer_returns403() throws Exception {
        long otroStaffId = 99L;
        TestSecurityUtils.authenticateAsStaff(CAJERO_UUID, otroStaffId, List.of("pos.vender"));
        when(supabaseAuthFacade.staffPerfilId()).thenReturn(java.util.Optional.of(otroStaffId));

        mockMvc.perform(get("/api/v1/usuarios-admin/" + STAFF_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IDOR: PUT /api/v1/usuarios-admin/{id} de otro staff, sin 'usuarios.editar' → 403")
    void actualizar_otroStaffSinPermisoEditar_returns403() throws Exception {
        long otroStaffId = 99L;
        TestSecurityUtils.authenticateAsStaff(CAJERO_UUID, otroStaffId, List.of("pos.vender"));
        when(supabaseAuthFacade.staffPerfilId()).thenReturn(java.util.Optional.of(otroStaffId));

        mockMvc.perform(put("/api/v1/usuarios-admin/" + STAFF_ID)
                        .with(csrf())
                        .contentType("application/json")
                        .content(cuerpoActualizar()))
                .andExpect(status().isForbidden());
    }

    private static String cuerpoActualizar() {
        return "{\"nombre\":\"Juan Perez\",\"telefono\":\"999999999\"}";
    }
}
