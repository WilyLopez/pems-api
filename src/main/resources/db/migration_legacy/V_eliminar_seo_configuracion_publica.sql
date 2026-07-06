-- Elimina el apartado SEO (metatags y Open Graph) de configuracion_publica.
-- Cada pagina publica ya define su propio title/description en codigo.
-- Ejecutar manualmente en Supabase DESPUES de desplegar backend/frontend sin estos campos.
-- Idempotente.
ALTER TABLE configuracion_publica
    DROP COLUMN IF EXISTS meta_title,
    DROP COLUMN IF EXISTS meta_description,
    DROP COLUMN IF EXISTS open_graph_title,
    DROP COLUMN IF EXISTS open_graph_description,
    DROP COLUMN IF EXISTS open_graph_image_path;
