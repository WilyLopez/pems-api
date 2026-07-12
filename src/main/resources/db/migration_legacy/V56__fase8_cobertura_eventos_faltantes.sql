UPDATE tipo_notificacion
SET es_obligatoria = TRUE
WHERE codigo = 'CAJA_CIERRE_DISCREPANCIA';

UPDATE tipo_notificacion
SET plantilla_titulo  = 'Recordatorio de tu visita',
    plantilla_mensaje = 'Te esperamos mañana {fecha} en {sede}. Tu ticket es {ticket}.'
WHERE codigo = 'RESERVA_RECORDATORIO'
  AND plantilla_titulo IS NULL;

UPDATE tipo_notificacion
SET plantilla_titulo  = 'Tu evento es el {fecha}'
WHERE codigo = 'EVENTO_RECORDATORIO_3DIAS'
  AND plantilla_titulo IS NULL;

INSERT INTO tipo_notificacion
    (codigo, modulo, nombre, descripcion, destinatario_default, canales_default,
     plantilla_titulo, plantilla_mensaje, prioridad, es_sistema, es_obligatoria, orden)
VALUES
    ('CAJA_APERTURA',
     'caja', 'Caja aperturada', 'Notificación de bajo ruido cuando se abre una sesión de caja',
     'ADMIN', ARRAY['IN_APP'],
     'Caja aperturada — {sede}',
     '{usuario} abrió una caja ({tipo}) en {sede} con saldo inicial S/ {saldoInicial}.',
     'BAJA', TRUE, FALSE, 200),

    ('CAJA_MOVIMIENTO_GRANDE',
     'caja', 'Movimiento de caja elevado', 'Movimiento manual de caja por encima del monto configurado',
     'ADMIN', ARRAY['IN_APP'],
     'Movimiento elevado — S/ {monto}',
     'Se registró un movimiento de {tipo} por S/ {monto} en {sede}. Concepto: {concepto}.',
     'NORMAL', TRUE, FALSE, 201),

    ('RESERVA_REPROGRAMADA',
     'reserva', 'Reserva reprogramada', 'Aviso al cliente cuando el staff reprograma su reserva',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Tu reserva cambió de fecha',
     'Tu reserva {ticket} fue reprogramada del {fechaAnterior} al {fechaNueva}.',
     'ALTA', TRUE, TRUE, 202),

    ('RESERVA_INGRESO_CONFIRMADO',
     'reserva', 'Ingreso confirmado', 'Registro de confirmación de asistencia al ingresar con el ticket',
     'CLIENTE', ARRAY['IN_APP'],
     'Ingreso confirmado',
     'Tu ingreso con el ticket {ticket} fue registrado. ¡Disfruta tu visita!',
     'BAJA', TRUE, FALSE, 203);
