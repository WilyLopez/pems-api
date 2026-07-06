-- Traslada el mapa embebido de Google Maps de configuracion_publica a sede,
-- ya que es un dato del local (sede), no de la configuración pública del sitio.
-- Ejecutar manualmente en Supabase. Idempotente.
ALTER TABLE sede
    ADD COLUMN IF NOT EXISTS google_maps_embed_url TEXT;

COMMENT ON COLUMN sede.google_maps_embed_url IS
    'URL del iframe de Google Maps (Compartir > Insertar un mapa) para el local.';

-- Copia el valor ya configurado en configuracion_publica hacia la sede,
-- solo si la sede aún no tiene uno propio (negocio de una sola sede).
UPDATE sede
SET google_maps_embed_url = cp.google_maps_embed_url
FROM configuracion_publica cp
WHERE sede.google_maps_embed_url IS NULL
  AND cp.google_maps_embed_url IS NOT NULL;

ALTER TABLE configuracion_publica
    DROP COLUMN IF EXISTS direccion,
    DROP COLUMN IF EXISTS google_maps_url,
    DROP COLUMN IF EXISTS google_maps_embed_url;
