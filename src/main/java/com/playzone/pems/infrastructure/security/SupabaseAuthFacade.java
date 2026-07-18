package com.playzone.pems.infrastructure.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SupabaseAuthFacade {

    public Optional<UUID> usuarioActualId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID id) {
            return Optional.of(id);
        }
        return Optional.empty();
    }

    public Optional<SupabaseAuthContext> contextoActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof SupabaseAuthContext ctx) {
            return Optional.of(ctx);
        }
        return Optional.empty();
    }

    public boolean tienePermiso(String codigoPermiso) {
        return contextoActual()
                .map(ctx -> ctx.permisos().contains(codigoPermiso))
                .orElse(false);
    }

    public boolean tieneRol(String codigoRol) {
        return contextoActual()
                .map(ctx -> ctx.roles().contains(codigoRol))
                .orElse(false);
    }

    public Optional<Long> clientePerfilId() {
        return contextoActual().map(SupabaseAuthContext::clientePerfilId);
    }

    public Optional<Long> staffPerfilId() {
        return contextoActual().map(SupabaseAuthContext::staffPerfilId);
    }

    public Optional<Long> sedeId() {
        return contextoActual().map(SupabaseAuthContext::sedeId);
    }
}
