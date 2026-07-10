ALTER TABLE registro_ingreso
    ADD COLUMN naturaleza TEXT NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN registro_anulado_id BIGINT REFERENCES registro_ingreso(id) ON DELETE RESTRICT;

ALTER TABLE registro_ingreso
    ADD CONSTRAINT ck_registro_ingreso_naturaleza CHECK (naturaleza IN ('NORMAL','CONTRAASIENTO')),
    ADD CONSTRAINT ck_registro_ingreso_anulacion CHECK (
        (naturaleza = 'NORMAL' AND registro_anulado_id IS NULL)
     OR (naturaleza = 'CONTRAASIENTO' AND registro_anulado_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_registro_ingreso_anulado
    ON registro_ingreso (registro_anulado_id)
    WHERE registro_anulado_id IS NOT NULL;

ALTER TABLE registro_egreso
    ADD COLUMN naturaleza TEXT NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN registro_anulado_id BIGINT REFERENCES registro_egreso(id) ON DELETE RESTRICT;

ALTER TABLE registro_egreso
    ADD CONSTRAINT ck_registro_egreso_naturaleza CHECK (naturaleza IN ('NORMAL','CONTRAASIENTO')),
    ADD CONSTRAINT ck_registro_egreso_anulacion CHECK (
        (naturaleza = 'NORMAL' AND registro_anulado_id IS NULL)
     OR (naturaleza = 'CONTRAASIENTO' AND registro_anulado_id IS NOT NULL)
    );

CREATE UNIQUE INDEX uk_registro_egreso_anulado
    ON registro_egreso (registro_anulado_id)
    WHERE registro_anulado_id IS NOT NULL;
