INSERT INTO tipo_notificacion
    (codigo, modulo, nombre, descripcion, destinatario_default, canales_default,
     plantilla_titulo, plantilla_mensaje, prioridad, es_sistema, es_obligatoria, orden)
VALUES
    ('RESERVA_PENDIENTE_CAJA',
     'reserva', 'Reserva registrada, pagar en caja', 'Reserva creada eligiendo pago en caja',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Reserva registrada — paga en caja',
     'Tu reserva {ticket} para el {fecha} quedo registrada. Paga S/ {total} en caja al llegar.',
     'NORMAL', TRUE, FALSE, 210),

    ('RESERVA_PENDIENTE_YAPE',
     'reserva', 'Reserva registrada, falta comprobante', 'Reserva creada eligiendo pago por Yape',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Reserva registrada — sube tu comprobante',
     'Tu reserva {ticket} para el {fecha} quedo registrada. Sube tu comprobante Yape por S/ {total} desde tu portal.',
     'NORMAL', TRUE, FALSE, 211),

    ('COMPROBANTE_EN_REVISION',
     'reserva', 'Comprobante recibido', 'El cliente subio un comprobante Yape, pendiente de revision',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Comprobante recibido',
     'Recibimos tu comprobante para la reserva {ticket}. Te avisaremos cuando sea aprobado o rechazado.',
     'NORMAL', TRUE, FALSE, 212),

    ('COMPROBANTE_PARA_REVISAR',
     'reserva', 'Comprobante por revisar', 'Aviso interno cuando un cliente sube un comprobante Yape',
     'ADMIN', ARRAY['IN_APP'],
     'Comprobante por revisar — {cliente}',
     '{cliente} subio un comprobante para la reserva {ticket}. Revisalo en el panel.',
     'NORMAL', TRUE, FALSE, 213),

    ('RESERVA_REPROGRAMADA_CON_PAGO',
     'reserva', 'Reserva reprogramada con pago adicional', 'Aviso al cliente cuando su reprogramacion exige un pago adicional',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Tu reserva cambio de fecha — falta un pago adicional',
     'Tu reserva {ticket} fue reprogramada del {fechaAnterior} al {fechaNueva}. Falta un pago adicional de S/ {montoAdicional}.',
     'ALTA', TRUE, FALSE, 214);
