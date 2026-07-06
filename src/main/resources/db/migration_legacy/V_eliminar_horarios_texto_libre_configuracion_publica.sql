-- Elimina el horario en texto libre de configuracion_publica.
-- El sitio publico pasa a calcular el horario desde configuracion_calendario
-- (horaApertura, horaCierre, diasOperacion), fuente unica gestionada en Configuracion -> Operacion.
-- Ejecutar manualmente en Supabase DESPUES de desplegar backend/frontend sin estos campos.
-- Idempotente.
ALTER TABLE configuracion_publica
    DROP COLUMN IF EXISTS horario_semana,
    DROP COLUMN IF EXISTS horario_fin_semana;
