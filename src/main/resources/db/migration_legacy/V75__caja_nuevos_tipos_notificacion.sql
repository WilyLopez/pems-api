INSERT INTO tipo_notificacion
    (codigo, modulo, nombre, descripcion, destinatario_default, canales_default,
     plantilla_titulo, plantilla_mensaje, prioridad, es_sistema, es_obligatoria, orden)
VALUES
    ('CAJA_ARQUEO_DISCREPANCIA',
     'caja', 'Discrepancia en arqueo de caja', 'Diferencia entre monto esperado y monto contado en un arqueo parcial',
     'ADMIN', ARRAY['IN_APP'],
     'Discrepancia en arqueo — {sede}',
     'Un arqueo de caja en {sede} tiene una diferencia de S/ {diferencia}.',
     'NORMAL', TRUE, FALSE, 220),

    ('CAJA_SESION_PROLONGADA',
     'caja', 'Caja abierta por mucho tiempo', 'Sesion de caja abierta mas alla del umbral de horas configurado',
     'ADMIN', ARRAY['IN_APP'],
     'Caja abierta hace mas de {horas}h — {sede}',
     '{usuario} tiene una caja ({tipo}) abierta en {sede} desde hace mas de {horas} horas.',
     'ALTA', TRUE, FALSE, 221),

    ('CAJA_STAFF_INACTIVO_CON_SESION',
     'caja', 'Caja abierta de personal desactivado', 'Un usuario dado de baja mantiene una sesion de caja abierta',
     'ADMIN', ARRAY['IN_APP','EMAIL'],
     'Caja de personal desactivado — {sede}',
     '{usuario} fue desactivado y aun mantiene una caja ({tipo}) abierta en {sede}. Revisa si corresponde un cierre forzado.',
     'CRITICA', TRUE, TRUE, 222)
ON CONFLICT (codigo) DO NOTHING;
