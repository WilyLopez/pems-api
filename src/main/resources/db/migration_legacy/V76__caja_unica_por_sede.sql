-- Fase 1 del plan "Caja unica por sede":
-- Reemplaza la unicidad por (sede_id, tipo) por unicidad estricta por sede_id.
-- Ya no se permite una caja CAJERO y una ADMINISTRATIVA abiertas en paralelo
-- en la misma sede: solo puede haber una sesion ABIERTA por sede a la vez.

DROP INDEX IF EXISTS uk_sesion_caja_sede_tipo_abierta;

CREATE UNIQUE INDEX uk_sesion_caja_sede_abierta
    ON sesion_caja (sede_id)
    WHERE estado_codigo = 'ABIERTA';
