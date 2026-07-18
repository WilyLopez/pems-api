CREATE OR REPLACE FUNCTION app.usuario_tiene_rol(p_rol_codigo TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM usuario_rol ur
        JOIN rol r ON r.codigo = ur.rol_codigo
        WHERE ur.usuario_id = auth.uid()
          AND ur.rol_codigo = p_rol_codigo
          AND r.activo = TRUE
    );
$$;


CREATE OR REPLACE FUNCTION app.usuario_tiene_permiso(p_permiso_codigo TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM usuario_rol ur
        JOIN rol r ON r.codigo = ur.rol_codigo
        JOIN rol_permiso rp ON rp.rol_codigo = ur.rol_codigo
        JOIN permiso p ON p.codigo = rp.permiso_codigo
        WHERE ur.usuario_id = auth.uid()
          AND rp.permiso_codigo = p_permiso_codigo
          AND r.activo = TRUE
          AND p.activo = TRUE
    );
$$;
