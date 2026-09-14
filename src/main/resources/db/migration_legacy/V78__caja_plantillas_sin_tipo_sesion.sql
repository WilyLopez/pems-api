-- Fase 6 del plan "Caja unica por sede": el codigo deja de enviar el
-- placeholder {tipo} (CAJERO/ADMINISTRATIVA) para las notificaciones de
-- sesion de caja, porque ese tipo ya no existe. Se actualizan las
-- plantillas para no dejar el placeholder sin reemplazo.
--
-- CAJA_MOVIMIENTO_GRANDE no se toca: su {tipo} es el tipo de movimiento
-- (INGRESO/EGRESO), que no tiene relacion con TipoSesionCaja y sigue
-- enviandose igual.

UPDATE tipo_notificacion
SET plantilla_mensaje = '{usuario} abrió una caja en {sede} con saldo inicial S/ {saldoInicial}.'
WHERE codigo = 'CAJA_APERTURA';

UPDATE tipo_notificacion
SET plantilla_mensaje = '{usuario} tiene una caja abierta en {sede} desde hace mas de {horas} horas.'
WHERE codigo = 'CAJA_SESION_PROLONGADA';

UPDATE tipo_notificacion
SET plantilla_mensaje = '{usuario} fue desactivado y aun mantiene una caja abierta en {sede}. Revisa si corresponde un cierre forzado.'
WHERE codigo = 'CAJA_STAFF_INACTIVO_CON_SESION';
