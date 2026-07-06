-- Elimina configuración que no se aplica en el sitio:
--   colores institucionales, metaKeywords, Analytics y Pixel.
-- Ejecutar manualmente en Supabase DESPUÉS de desplegar el backend sin estos campos.
-- Idempotente.
ALTER TABLE configuracion_publica
    DROP COLUMN IF EXISTS color_primario,
    DROP COLUMN IF EXISTS color_secundario,
    DROP COLUMN IF EXISTS meta_keywords,
    DROP COLUMN IF EXISTS google_analytics_id,
    DROP COLUMN IF EXISTS meta_pixel_id;
