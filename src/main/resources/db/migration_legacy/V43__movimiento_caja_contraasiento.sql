ALTER TABLE movimiento_caja
    ADD COLUMN naturaleza TEXT NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN movimiento_anulado_id BIGINT REFERENCES movimiento_caja(id) ON DELETE RESTRICT;

ALTER TABLE movimiento_caja
    ADD CONSTRAINT ck_movimiento_caja_naturaleza CHECK (naturaleza IN ('NORMAL','CONTRAASIENTO')),
    ADD CONSTRAINT ck_movimiento_caja_anulacion CHECK (
        (naturaleza = 'NORMAL' AND movimiento_anulado_id IS NULL)
     OR (naturaleza = 'CONTRAASIENTO' AND movimiento_anulado_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_movimiento_caja_anulado
    ON movimiento_caja (movimiento_anulado_id)
    WHERE movimiento_anulado_id IS NOT NULL;
