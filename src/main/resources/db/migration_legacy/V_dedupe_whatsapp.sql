-- Deduplicar WhatsApp: la fuente única es configuracion_publica.whatsapp.
-- La clave WHATSAPP_NUMERO de configuracion_global no la usa nadie (código verificado).
-- Ejecutar manualmente en Supabase. Idempotente.
DELETE FROM configuracion_global WHERE clave = 'WHATSAPP_NUMERO';
