-- Branding configurable: logo secundario y 2 mascotas.
-- (logo_path y favicon_path ya existen.)
-- Ejecutar manualmente en Supabase. Idempotente.
ALTER TABLE configuracion_publica
    ADD COLUMN IF NOT EXISTS logo_secundario_path TEXT,
    ADD COLUMN IF NOT EXISTS mascota_1_path       TEXT,
    ADD COLUMN IF NOT EXISTS mascota_2_path       TEXT;
