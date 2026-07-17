ALTER TABLE tarifa
    ADD COLUMN duracion_minutos INT NULL;

ALTER TABLE tarifa
    ADD CONSTRAINT ck_tarifa_duracion CHECK (duracion_minutos IS NULL OR duracion_minutos > 0);

ALTER TABLE reserva
    ADD COLUMN duracion_historica_minutos INT NULL,
    ADD COLUMN permanencia_fin_at TIMESTAMPTZ NULL,
    ADD COLUMN salida_real_at TIMESTAMPTZ NULL;

ALTER TABLE reserva
    ADD CONSTRAINT ck_reserva_permanencia CHECK (permanencia_fin_at IS NULL OR ingreso_at IS NOT NULL);

INSERT INTO estado_reserva (codigo, nombre, descripcion, es_terminal, orden)
VALUES ('VENCIDA', 'Vencida', 'No se presento dentro del plazo, cupo liberado automaticamente', FALSE, 6);
