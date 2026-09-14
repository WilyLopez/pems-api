INSERT INTO configuracion_global (clave, valor, tipo_dato, descripcion, es_secreto)
VALUES ('CAJA_UMBRAL_HORAS_ABIERTA', '12', 'NUMERO',
        'Horas que una sesion de caja puede permanecer abierta antes de notificar una alerta',
        false)
ON CONFLICT (clave) DO NOTHING;
