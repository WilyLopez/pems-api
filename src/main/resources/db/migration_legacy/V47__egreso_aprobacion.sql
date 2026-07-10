ALTER TABLE registro_egreso
    ADD COLUMN estado_aprobacion TEXT NOT NULL DEFAULT 'APROBADO',
    ADD COLUMN aprobado_por UUID REFERENCES perfil_usuario(id),
    ADD COLUMN fecha_aprobacion TIMESTAMPTZ,
    ADD COLUMN motivo_rechazo TEXT;

ALTER TABLE registro_egreso
    ADD CONSTRAINT ck_registro_egreso_estado_aprobacion CHECK (
        estado_aprobacion IN ('APROBADO','PENDIENTE_APROBACION','RECHAZADO')
    );

CREATE INDEX idx_registro_egreso_estado_aprobacion
    ON registro_egreso (sede_id, estado_aprobacion)
    WHERE estado_aprobacion = 'PENDIENTE_APROBACION';

INSERT INTO configuracion_global (clave, valor, tipo_dato, descripcion, es_secreto)
VALUES ('EGRESO_UMBRAL_APROBACION', '500', 'NUMERO',
        'Monto en soles a partir del cual un egreso requiere aprobacion de un segundo usuario antes de afectar la caja',
        false)
ON CONFLICT (clave) DO NOTHING;
