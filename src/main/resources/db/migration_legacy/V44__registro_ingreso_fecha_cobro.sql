ALTER TABLE registro_ingreso
    ADD COLUMN fecha_cobro DATE NOT NULL DEFAULT (timezone('America/Lima', now()))::date;

CREATE INDEX idx_registro_ingreso_sede_fecha_cobro
    ON registro_ingreso (sede_id, fecha_cobro);
