DO $$
DECLARE
    v_constraint_usuario TEXT;
    v_constraint_cliente TEXT;
BEGIN
    SELECT tc.constraint_name INTO v_constraint_usuario
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
    WHERE tc.table_schema = 'public'
      AND tc.table_name = 'notificacion'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'destinatario_usuario_id';

    IF v_constraint_usuario IS NOT NULL THEN
        EXECUTE format('ALTER TABLE notificacion DROP CONSTRAINT %I', v_constraint_usuario);
    END IF;

    SELECT tc.constraint_name INTO v_constraint_cliente
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
        ON tc.constraint_name = kcu.constraint_name
        AND tc.table_schema = kcu.table_schema
    WHERE tc.table_schema = 'public'
      AND tc.table_name = 'notificacion'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'destinatario_cliente_id';

    IF v_constraint_cliente IS NOT NULL THEN
        EXECUTE format('ALTER TABLE notificacion DROP CONSTRAINT %I', v_constraint_cliente);
    END IF;
END $$;

ALTER TABLE notificacion
    ADD CONSTRAINT notificacion_destinatario_usuario_id_fkey
    FOREIGN KEY (destinatario_usuario_id) REFERENCES perfil_usuario(id) ON DELETE SET NULL;

ALTER TABLE notificacion
    ADD CONSTRAINT notificacion_destinatario_cliente_id_fkey
    FOREIGN KEY (destinatario_cliente_id) REFERENCES cliente_perfil(id) ON DELETE SET NULL;

ALTER TABLE notificacion DROP CONSTRAINT IF EXISTS ck_notif_destinatario;

ALTER TABLE notificacion ADD CONSTRAINT ck_notif_destinatario CHECK (
    NOT (destinatario_usuario_id IS NOT NULL AND destinatario_cliente_id IS NOT NULL)
);

CREATE OR REPLACE FUNCTION app.limpiar_envio_email_antiguos()
RETURNS INT
LANGUAGE plpgsql
AS $$
DECLARE
    v_eliminados INT;
BEGIN
    DELETE FROM envio_email
    WHERE estado IN ('ENVIADO', 'REBOTADO', 'CANCELADO')
      AND COALESCE(enviado_at, created_at) < NOW() - INTERVAL '180 days';
    GET DIAGNOSTICS v_eliminados = ROW_COUNT;
    RETURN v_eliminados;
END;
$$;

CREATE OR REPLACE FUNCTION app.mantenimiento_diario()
RETURNS TABLE (tarea TEXT, registros_afectados INT)
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_anio INT;
    v_mes  INT;
BEGIN
    v_anio := EXTRACT(YEAR FROM NOW() + INTERVAL '2 months')::INT;
    v_mes  := EXTRACT(MONTH FROM NOW() + INTERVAL '2 months')::INT;
    PERFORM app.crear_particion_auditoria(v_anio, v_mes);
    tarea := 'particion_auditoria_creada';
    registros_afectados := 1;
    RETURN NEXT;

    tarea := 'cache_dni_eliminados';
    registros_afectados := app.limpiar_cache_dni_vencido();
    RETURN NEXT;

    tarea := 'notificaciones_expiradas_eliminadas';
    registros_afectados := app.limpiar_notificaciones_expiradas();
    RETURN NEXT;

    tarea := 'cliente_tokens_eliminados';
    registros_afectados := app.limpiar_cliente_tokens_vencidos();
    RETURN NEXT;

    tarea := 'envio_email_antiguos_eliminados';
    registros_afectados := app.limpiar_envio_email_antiguos();
    RETURN NEXT;
END;
$$;
