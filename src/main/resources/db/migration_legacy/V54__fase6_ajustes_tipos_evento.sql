UPDATE tipo_notificacion
SET canales_default = ARRAY['IN_APP']::TEXT[]
WHERE codigo = 'PAGO_ADELANTO_CONFIRMADO';

UPDATE tipo_notificacion
SET plantilla_titulo  = 'Pago registrado — {evento}',
    plantilla_mensaje = 'Se registró un pago de S/ {monto} para el evento de {cliente}. Saldo pendiente: S/ {saldo}.'
WHERE codigo = 'EVENTO_SALDO_RECIBIDO';
