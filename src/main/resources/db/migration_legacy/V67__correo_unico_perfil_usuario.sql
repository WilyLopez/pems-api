DROP INDEX idx_perfil_usuario_correo;

CREATE UNIQUE INDEX idx_perfil_usuario_correo
    ON perfil_usuario (correo)
    WHERE deleted_at IS NULL;
