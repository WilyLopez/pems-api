package com.playzone.pems.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playzone.pems.domain.usuario.repository.ClientePerfilRepository;
import com.playzone.pems.domain.usuario.repository.PerfilUsuarioRepository;
import com.playzone.pems.domain.usuario.repository.UsuarioRolRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SupabaseJwtFilter extends OncePerRequestFilter {

    private final SupabaseJwksProvider    jwksProvider;
    private final UsuarioRolRepository    usuarioRolRepository;
    private final PerfilUsuarioRepository perfilUsuarioRepository;
    private final ClientePerfilRepository clientePerfilRepository;
    private final com.playzone.pems.domain.usuario.repository.StaffPerfilRepository staffPerfilRepository;
    private final ObjectMapper            objectMapper;

    private record CachedAuthorities(
            List<String> roles,
            List<String> permisos,
            Long clientePerfilId,
            Long staffPerfilId,
            Long sedeId,
            boolean debeCambiarPassword,
            boolean esActivo,
            boolean estaBloqueado,
            long cachedAt
    ) {}

    private final ConcurrentHashMap<UUID, CachedAuthorities> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_MS = 60_000L;

    private Claims validarToken(String token) {
        return Jwts.parser()
                .keyLocator(header -> {
                    String kid = (String) header.get("kid");
                    if (kid == null) {
                        throw new JwtException("JWT sin kid en header");
                    }
                    return jwksProvider.resolverClavePorKid(kid);
                })
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || authHeader.isBlank()
                || !authHeader.toLowerCase().startsWith("bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = validarToken(token);
        } catch (ExpiredJwtException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "expired_token",
                    "El token ha expirado. Inicia sesion nuevamente.");
            return;
        } catch (JwtException | IllegalStateException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token", e.getMessage());
            return;
        }

        Set<String> audience = claims.getAudience();
        if (audience == null || !audience.contains("authenticated")) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token", "Token audience invalido");
            return;
        }

        UUID userId;
        try {
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "invalid_token", "Subject invalido");
            return;
        }

        String email     = claims.get("email", String.class);
        String role      = claims.get("role",  String.class);
        long   expiresAt = claims.getExpiration().getTime();

        CachedAuthorities auth = resolveAuthorities(userId);
        if (auth == null) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "user_not_provisioned",
                    "Usuario autenticado pero sin perfil en el sistema. Contacte al administrador.");
            return;
        }

        if (!auth.esActivo()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "account_inactive",
                    "Cuenta desactivada. Contacte al administrador.");
            return;
        }

        if (auth.estaBloqueado()) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "account_locked",
                    "Cuenta bloqueada temporalmente por seguridad.");
            return;
        }

        if (auth.debeCambiarPassword() && !request.getRequestURI().equals("/api/v1/usuarios/me/password")) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "password_change_required",
                    "Debe cambiar su contraseña antes de continuar.");
            return;
        }

        List<SimpleGrantedAuthority> grantedAuthorities = auth.permisos().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        SupabaseAuthContext ctx = new SupabaseAuthContext(
                userId, email, role, auth.roles(), auth.permisos(), auth.clientePerfilId(),
                auth.staffPerfilId(), auth.sedeId(), expiresAt
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, grantedAuthorities);
        authentication.setDetails(ctx);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.setAttribute("supabaseAuthContext", ctx);

        filterChain.doFilter(request, response);
    }

    private CachedAuthorities resolveAuthorities(UUID userId) {
        CachedAuthorities cached = cache.get(userId);
        if (cached != null && System.currentTimeMillis() - cached.cachedAt() < CACHE_TTL_MS) {
            return cached;
        }

        if (perfilUsuarioRepository.buscarPorId(userId).isEmpty()) {
            return null;
        }

        List<String> roles    = usuarioRolRepository.listarCodigosRolPorUsuario(userId);
        List<String> permisos = usuarioRolRepository.listarCodigosPermisoPorUsuario(userId);
        java.util.Optional<com.playzone.pems.domain.usuario.model.ClientePerfil> cliente =
                clientePerfilRepository.buscarPorUsuarioIdIncluyendoInactivos(userId);
        Long clientePerfilId = cliente.filter(cp -> cp.getDeletedAt() == null)
                .map(com.playzone.pems.domain.usuario.model.ClientePerfil::getId)
                .orElse(null);

        boolean debeCambiarPassword = false;
        boolean esActivo             = cliente.isEmpty() || cliente.get().getDeletedAt() == null;
        boolean estaBloqueado       = false;
        Long    sedeId               = null;
        Long    staffPerfilId       = null;

        java.util.Optional<com.playzone.pems.domain.usuario.model.StaffPerfil> staff = staffPerfilRepository.buscarPorUsuarioId(userId);
        if (staff.isPresent()) {
            debeCambiarPassword = staff.get().isDebeCambiarContrasena();
            esActivo             = staff.get().isEsActivo();
            estaBloqueado       = staff.get().getBloqueadoHasta() != null
                    && staff.get().getBloqueadoHasta().isAfter(java.time.OffsetDateTime.now());
            sedeId               = staff.get().getSedeId();
            staffPerfilId       = staff.get().getId();
        }

        CachedAuthorities fresh = new CachedAuthorities(roles, permisos, clientePerfilId, staffPerfilId, sedeId,
                debeCambiarPassword, esActivo, estaBloqueado,
                System.currentTimeMillis());


        boolean provisionado = clientePerfilId != null || staff.isPresent();
        if (provisionado) {
            cache.put(userId, fresh);
        } else {
            cache.remove(userId);
        }
        return fresh;
    }

    private void writeError(HttpServletResponse response, int status,
                            String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        Map.of("error", error, "message", message != null ? message : ""))
        );
    }
}
