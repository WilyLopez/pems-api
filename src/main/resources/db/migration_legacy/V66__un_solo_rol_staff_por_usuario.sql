CREATE UNIQUE INDEX idx_usuario_rol_staff_unico
    ON usuario_rol (usuario_id)
    WHERE rol_codigo IN ('ADMIN', 'CAJERO', 'SUPERADMIN');
