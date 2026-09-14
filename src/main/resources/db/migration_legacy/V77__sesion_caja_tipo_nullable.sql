-- Fase 6 (paso 1 de 2) del plan "Caja unica por sede":
-- Antes de desplegar el codigo que deja de escribir la columna tipo,
-- se le quita el NOT NULL para que las nuevas aperturas de caja no
-- fallen mientras el codigo (que ya no envia ese valor) llega a
-- produccion. Este paso es seguro, reversible y puede aplicarse en
-- cualquier momento, independientemente del despliegue del backend.
--
-- El paso 2 (destructivo: elimina la columna por completo) es el
-- script 2026-09-14_script_eliminar_caja_administrativa.sql en
-- _analisis/mejoras/ -- ese si debe correr DESPUES de que el backend
-- con estos cambios ya este desplegado.

ALTER TABLE sesion_caja ALTER COLUMN tipo DROP NOT NULL;
