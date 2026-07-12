INSERT INTO tipo_notificacion
    (codigo, modulo, nombre, descripcion, destinatario_default, canales_default,
     plantilla_titulo, plantilla_mensaje, prioridad, es_sistema, es_obligatoria, orden)
VALUES
    ('CAMBIO_PASSWORD',
     'seguridad', 'Cambio de contraseña', 'Aviso de seguridad tras un cambio de contraseña exitoso',
     'ADMIN', ARRAY['IN_APP','EMAIL'],
     'Tu contraseña fue actualizada',
     'Tu contraseña se actualizó correctamente. Si no reconoces este cambio, contacta a soporte de inmediato.',
     'ALTA', TRUE, TRUE, 190),

    ('CAMBIO_CORREO_SOLICITADO',
     'seguridad', 'Confirmación de cambio de correo', 'Enlace de confirmación enviado al nuevo correo solicitado',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Confirma tu nuevo correo',
     'Solicitaste cambiar tu correo de contacto a {correoNuevo}. Confirma el cambio desde el enlace que enviamos a esa dirección.',
     'NORMAL', TRUE, TRUE, 191),

    ('CAMBIO_CORREO_ALERTA',
     'seguridad', 'Alerta de solicitud de cambio de correo', 'Aviso informativo enviado al correo actual ante una solicitud de cambio',
     'CLIENTE', ARRAY['EMAIL'],
     'Solicitud de cambio de correo',
     'Se solicitó cambiar el correo de contacto de tu cuenta a {correoNuevo}. Si no fuiste tú, contáctanos de inmediato.',
     'ALTA', TRUE, TRUE, 192),

    ('CAMBIO_CORREO_CONFIRMADO',
     'seguridad', 'Correo actualizado', 'Aviso de que el cambio de correo se aplicó correctamente',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Correo actualizado',
     'Tu correo de contacto se actualizó correctamente a {correoNuevo}.',
     'NORMAL', TRUE, FALSE, 193),

    ('USUARIO_BLOQUEADO',
     'seguridad', 'Cuenta bloqueada', 'Aviso de bloqueo de una cuenta de staff (manual o por intentos fallidos)',
     'ADMIN', ARRAY['IN_APP','EMAIL'],
     'Cuenta bloqueada — {nombre}',
     'La cuenta de {nombre} fue bloqueada. Motivo: {motivo}.',
     'ALTA', TRUE, TRUE, 194),

    ('USUARIO_DESBLOQUEADO',
     'seguridad', 'Cuenta desbloqueada', 'Aviso de desbloqueo manual de una cuenta de staff',
     'ADMIN', ARRAY['IN_APP','EMAIL'],
     'Cuenta desbloqueada — {nombre}',
     'La cuenta de {nombre} fue desbloqueada y puede volver a iniciar sesión.',
     'NORMAL', TRUE, FALSE, 195),

    ('CAMBIO_ROL',
     'seguridad', 'Cambio de rol', 'Aviso de cambio de rol de una cuenta de staff',
     'ADMIN', ARRAY['IN_APP','EMAIL'],
     'Cambio de rol — {nombre}',
     'El rol de {nombre} cambió de {rolAnterior} a {rolNuevo}.',
     'NORMAL', TRUE, FALSE, 196);
