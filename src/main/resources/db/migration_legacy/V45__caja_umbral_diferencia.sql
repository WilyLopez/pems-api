INSERT INTO configuracion_global (clave, valor, tipo_dato, descripcion, es_secreto)
VALUES ('CAJA_UMBRAL_DIFERENCIA', '0', 'NUMERO',
        'Umbral en soles de diferencia absoluta al cerrar caja a partir del cual se exige observacion',
        false)
ON CONFLICT (clave) DO NOTHING;
