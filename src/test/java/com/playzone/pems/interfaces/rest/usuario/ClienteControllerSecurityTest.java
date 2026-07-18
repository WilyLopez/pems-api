package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.application.usuario.port.in.*;
import com.playzone.pems.application.cms.port.in.RegistrarConsentimientoUseCase;
import com.playzone.pems.domain.storage.StoragePort;
import com.playzone.pems.infrastructure.security.SupabaseAuthFacade;
import com.playzone.pems.infrastructure.security.SupabaseJwtFilter;
import com.playzone.pems.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de Seguridad — ClienteController
 *
 * Categorías:
 *   1. Acceso sin autenticación       → HTTP 401
 *   2. Acceso con permisos insuficientes → HTTP 403
 *   3. Acceso correcto con permisos válidos → HTTP 2xx
 *   4. IDOR — acceso de un usuario a recursos ajenos
 */
@WebMvcTest(ClienteController.class)
@ActiveProfiles("test")
@DisplayName("Seguridad - ClienteController")
class ClienteControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    /* ---------- mocks requeridos por @WebMvcTest ---------- */
    @MockBean private RegistrarClientePerfilUseCase  registrarUseCase;
    @MockBean private RegistrarClientePublicoUseCase registrarPublicoUseCase;
    @MockBean private RegistrarConsentimientoUseCase consentimientoUseCase;
    @MockBean private ActualizarClientePerfilUseCase actualizarUseCase;
    @MockBean private ListarClientesPerfilUseCase    listarUseCase;
    @MockBean private ObtenerClientePerfilUseCase    obtenerUseCase;
    @MockBean private ActivarClientePerfilUseCase    activarUseCase;
    @MockBean private DesactivarClientePerfilUseCase desactivarUseCase;
    @MockBean private HacerVipPerfilUseCase          hacerVipUseCase;
    @MockBean private QuitarVipPerfilUseCase         quitarVipUseCase;
    @MockBean private RegistrarVisitaPerfilUseCase   visitaUseCase;
    @MockBean private ActualizarSegmentoPerfilUseCase segmentoUseCase;
    @MockBean private CompletarPerfilClienteUseCase  completarUseCase;
    @MockBean private ConfirmarCambioCorreoUseCase   confirmarCambioCorreoUseCase;
    @MockBean private SupabaseAuthFacade             supabaseAuthFacade;
    @MockBean private StoragePort                    storagePort;
    @MockBean private SupabaseJwtFilter              supabaseJwtFilter;

    private static final Long CLIENTE_ID = 42L;
    private static final UUID ADMIN_UUID = UUID.randomUUID();
    private static final UUID CLIENTE_UUID = UUID.randomUUID();

    @AfterEach
    void limpiarContexto() {
        TestSecurityUtils.clearAuthentication();
    }

    /* ═══════════════════════════════════════════════════════════════
       1. Sin autenticación → 401
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("GET /api/v1/clientes sin token → 401")
    void listar_sinAutenticacion_returns401() throws Exception {
        // Sin autenticación → filtro rechaza la petición con 401
        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/clientes/{id} sin token → 401")
    void obtener_sinAutenticacion_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/clientes/" + CLIENTE_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/clientes/{id}/activar sin token → 401")
    void activar_sinAutenticacion_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/clientes/" + CLIENTE_ID + "/activar"))
                .andExpect(status().isUnauthorized());
    }

    /* ═══════════════════════════════════════════════════════════════
       2. Sin permiso correcto → 403
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("POST /api/v1/clientes/{id}/activar con permiso 'cliente.ver' → 403")
    void activar_sinPermisoEditar_returns403() throws Exception {
        // Un usuario autenticado pero sin 'cliente.editar'
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, CLIENTE_ID, List.of("cliente.ver"));

        mockMvc.perform(post("/api/v1/clientes/" + CLIENTE_ID + "/activar"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/clientes/{id}/vip con permiso 'cliente.ver' → 403")
    void hacerVip_sinPermisoEditar_returns403() throws Exception {
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, CLIENTE_ID, List.of("cliente.ver"));

        mockMvc.perform(post("/api/v1/clientes/" + CLIENTE_ID + "/vip")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/clientes sin 'cliente.ver' → 403")
    void listar_sinPermisoVer_returns403() throws Exception {
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, CLIENTE_ID, List.of("pos.vender"));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isForbidden());
    }

    /* ═══════════════════════════════════════════════════════════════
       3. Acceso exitoso con permisos correctos → 2xx
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("POST /api/v1/clientes/{id}/activar con 'cliente.editar' → 200")
    void activar_conPermisoEditar_returns200() throws Exception {
        TestSecurityUtils.authenticateAsAdmin(ADMIN_UUID, List.of("cliente.editar"));
        doNothing().when(activarUseCase).activar(CLIENTE_ID);

        mockMvc.perform(post("/api/v1/clientes/" + CLIENTE_ID + "/activar"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/v1/clientes/{id}/desactivar con 'cliente.editar' → 200")
    void desactivar_conPermisoEditar_returns200() throws Exception {
        TestSecurityUtils.authenticateAsAdmin(ADMIN_UUID, List.of("cliente.editar"));
        doNothing().when(desactivarUseCase).desactivar(CLIENTE_ID);

        mockMvc.perform(post("/api/v1/clientes/" + CLIENTE_ID + "/desactivar"))
                .andExpect(status().isOk());
    }

    /* ═══════════════════════════════════════════════════════════════
       4. IDOR — un usuario no puede editar recursos ajenos
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("IDOR: PUT /api/v1/clientes/{id}/foto de otro cliente → 403")
    void subirFoto_otraPersona_returns403() throws Exception {
        // El usuario autenticado es el cliente 99, pero intenta editar el perfil 42
        long otroClienteId = 99L;
        // clientePerfilId en contexto = otroClienteId, no CLIENTE_ID
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, otroClienteId,
                List.of("cliente.editar"));

        // @PreAuthorize exige que #id == @supabaseAuthFacade.clientePerfilId() OR usuarios.gestionar
        // Como el context tiene clientePerfilId=99 != 42, debe ser 403
        mockMvc.perform(put("/api/v1/clientes/" + CLIENTE_ID + "/foto"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("IDOR: DELETE /api/v1/clientes/{id}/foto de otro cliente → 403")
    void eliminarFoto_otraPersona_returns403() throws Exception {
        long otroClienteId = 99L;
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, otroClienteId,
                List.of("cliente.editar"));

        mockMvc.perform(delete("/api/v1/clientes/" + CLIENTE_ID + "/foto"))
                .andExpect(status().isForbidden());
    }

    /* ═══════════════════════════════════════════════════════════════
       5. Escalada de privilegios — cliente no puede hacer vip propio
    ═══════════════════════════════════════════════════════════════ */

    @Test
    @DisplayName("Escalada: POST /api/v1/clientes/{id}/vip como cliente sin 'cliente.editar' → 403")
    void escalada_clienteHacerVipPropio_returns403() throws Exception {
        // Un cliente autenticado como sí mismo, pero sin el permiso 'cliente.editar'
        TestSecurityUtils.authenticateAsCliente(CLIENTE_UUID, CLIENTE_ID,
                List.of("cliente.ver"));

        mockMvc.perform(post("/api/v1/clientes/" + CLIENTE_ID + "/vip")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
