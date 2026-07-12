package com.playzone.pems.application.usuario.port.out;

import java.util.UUID;

public interface SupabaseAuthPort {

    /**
     * Crea un usuario en Supabase Auth mediante el endpoint correspondiente (signup o admin).
     * @param email Correo electrónico.
     * @param password Contraseña.
     * @param nombreCompleto Nombre completo para el metadata.
     * @param confirmarEmail Si se debe auto-confirmar el correo electrónico (requiere service_role).
     * @return El UUID del usuario creado.
     */
    UUID crearUsuario(String email, String password, String nombreCompleto, boolean confirmarEmail);

    /**
     * Autentica un usuario con email y password.
     * @return Mapa con la respuesta de Supabase (access_token, user_id, etc.)
     */
    java.util.Map<String, Object> login(String email, String password);

    /**
     * Actualiza los datos del usuario autenticado (incluyendo password).
     */
    void actualizarPassword(String accessToken, String newPassword);

    /**
     * Inicia el flujo de recuperación de contraseña.
     */
    void recuperarPassword(String email);

    /**
     * Establece la contraseña de un usuario existente mediante la Admin API (requiere service_role).
     */
    void establecerPasswordAdmin(UUID usuarioId, String nuevoPassword);
}
