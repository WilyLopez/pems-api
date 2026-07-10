ALTER TABLE gasto_operativo_diario
    ADD COLUMN naturaleza TEXT NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN gasto_anulado_id BIGINT REFERENCES gasto_operativo_diario(id) ON DELETE RESTRICT;

ALTER TABLE gasto_operativo_diario
    ADD CONSTRAINT ck_gasto_operativo_naturaleza CHECK (naturaleza IN ('NORMAL','CONTRAASIENTO')),
    ADD CONSTRAINT ck_gasto_operativo_anulacion CHECK (
        (naturaleza = 'NORMAL' AND gasto_anulado_id IS NULL)
     OR (naturaleza = 'CONTRAASIENTO' AND gasto_anulado_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_gasto_operativo_anulado
    ON gasto_operativo_diario (gasto_anulado_id)
    WHERE gasto_anulado_id IS NOT NULL;
