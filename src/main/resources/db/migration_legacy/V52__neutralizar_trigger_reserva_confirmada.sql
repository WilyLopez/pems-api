DROP TRIGGER IF EXISTS trg_reserva_notificar_confirmacion ON reserva;

UPDATE notificacion
SET expira_at = created_at + INTERVAL '30 days'
WHERE tipo_codigo = 'RESERVA_CONFIRMADA'
  AND expira_at IS NULL;

UPDATE tipo_notificacion
SET plantilla_titulo  = 'Reserva cancelada',
    plantilla_mensaje = 'Tu reserva del {fecha} fue cancelada. Motivo: {motivo}.'
WHERE codigo = 'RESERVA_CANCELADA'
  AND plantilla_titulo IS NULL;
