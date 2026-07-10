-- Consolidacion final: apertura_caja fue reemplazada por sesion_caja desde V42.
-- Todo el codigo de aplicacion usa sesion_caja_id desde la Fase 1; estas columnas
-- y la tabla quedaron como remanente historico sin escritura ni lectura.

ALTER TABLE movimiento_caja DROP COLUMN IF EXISTS apertura_caja_id;
ALTER TABLE arqueo_caja     DROP COLUMN IF EXISTS apertura_caja_id;

DROP TABLE IF EXISTS apertura_caja;
