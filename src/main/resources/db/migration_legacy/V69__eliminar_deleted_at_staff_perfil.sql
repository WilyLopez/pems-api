CREATE OR REPLACE FUNCTION app.sede_actual()
RETURNS BIGINT
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
    SELECT sede_id
    FROM staff_perfil
    WHERE usuario_id = auth.uid()
      AND es_activo = TRUE
    LIMIT 1;
$$;

ALTER TABLE staff_perfil DROP COLUMN deleted_at;
 