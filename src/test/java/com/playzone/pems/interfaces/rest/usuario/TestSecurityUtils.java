package com.playzone.pems.interfaces.rest.usuario;

import com.playzone.pems.infrastructure.security.SupabaseAuthContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Utilidad para inyectar contextos de autenticación simulados de Supabase en los tests.
 *
 * Permite construir escenarios de diferentes tipos de usuarios (cliente, staff, admin)
 * sin necesidad de un token JWT real ni conexión a Supabase.
 */
public class TestSecurityUtils {

    /**
     * Registra en el SecurityContext un usuario cliente con permisos específicos.
     *
     * @param userId          UUID del usuario autenticado
     * @param clientePerfilId ID del perfil de cliente en la BD
     * @param permisos        Lista de permisos (authorities) concedidos
     */
    public static void authenticateAsCliente(UUID userId, Long clientePerfilId, List<String> permisos) {
        authenticate(userId, "cliente@test.com", "authenticated", List.of("CLIENTE"),
                permisos, clientePerfilId, null);
    }

    /**
     * Registra en el SecurityContext un usuario administrador con permisos completos.
     *
     * @param userId   UUID del usuario administrador
     * @param permisos Lista de permisos administrativos
     */
    public static void authenticateAsAdmin(UUID userId, List<String> permisos) {
        authenticate(userId, "admin@test.com", "authenticated", List.of("SUPERADMIN"),
                permisos, null, 1L);
    }

    /**
     * Limpia el SecurityContext (simula un usuario anónimo / sin autenticar).
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticate(UUID userId, String email, String role,
                                     List<String> roles, List<String> permisos,
                                     Long clientePerfilId, Long sedeId) {
        SupabaseAuthContext ctx = new SupabaseAuthContext(
                userId, email, role, roles, permisos,
                clientePerfilId, sedeId,
                System.currentTimeMillis() + 3_600_000L
        );

        List<SimpleGrantedAuthority> authorities = permisos.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);
        auth.setDetails(ctx);

        SecurityContext secCtx = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(secCtx);
    }
}
