-- Edad del niño como configuración única del sistema.
-- Ejecutar manualmente en Supabase. Idempotente.
INSERT INTO configuracion_global (clave, valor, tipo_dato, descripcion, es_sistema, es_secreto)
VALUES
    ('EDAD_MIN_NINO', '1',  'NUMERO', 'Edad mínima de niño aceptada', TRUE, FALSE),
    ('EDAD_MAX_NINO', '8', 'NUMERO', 'Edad máxima de niño aceptada', TRUE, FALSE)
ON CONFLICT (clave) DO NOTHING;
