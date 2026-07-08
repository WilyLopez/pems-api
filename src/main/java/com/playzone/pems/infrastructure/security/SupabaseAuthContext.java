package com.playzone.pems.infrastructure.security;

import java.util.List;
import java.util.UUID;

public record SupabaseAuthContext(
        UUID userId,
        String email,
        String role,
        List<String> roles,
        List<String> permisos,
        Long clientePerfilId,
        Long sedeId,
        long expiresAt
) {}
