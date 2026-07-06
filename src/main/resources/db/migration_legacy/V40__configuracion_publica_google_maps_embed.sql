-- Agrega la URL de inserción (embed) de Google Maps para el mapa visible en /contacto.
-- Distinta de google_maps_url (que es el link normal usado en el botón "Cómo llegar").
-- Ejecutar manualmente en Supabase. Idempotente.
ALTER TABLE configuracion_publica
    ADD COLUMN IF NOT EXISTS google_maps_embed_url TEXT;

COMMENT ON COLUMN configuracion_publica.google_maps_embed_url IS
    'URL del iframe de Google Maps (Compartir > Insertar un mapa). No confundir con google_maps_url, que es el link externo del botón "Cómo llegar".';

-- Carga inicial con el embed ya provisto para "Kiki y Lala Juegos Infantiles y Eventos".
-- Solo aplica si el campo aún no tiene valor, para no pisar una edición posterior desde el admin.
UPDATE configuracion_publica
SET google_maps_embed_url = 'https://www.google.com/maps/embed?pb=!1m14!1m8!1m3!1d3961.856049832183!2d-79.8382024!3d-6.7873658!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x904cef8c152c20eb%3A0x1c034875bbd1c9e8!2sKiki%20y%20Lala%20Juegos%20Infantiles%20y%20Eventos!5e0!3m2!1ses-419!2spe!4v1783294404542!5m2!1ses-419!2spe'
WHERE google_maps_embed_url IS NULL;
