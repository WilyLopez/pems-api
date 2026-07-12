ALTER TABLE envio_email
    ADD COLUMN cuerpo_html TEXT;

ALTER TABLE envio_email
    ADD CONSTRAINT uq_envio_email_campana_cliente UNIQUE (campana_id, cliente_id);

DROP INDEX IF EXISTS idx_envio_email_estado;

CREATE INDEX idx_envio_email_estado
    ON envio_email (estado)
    WHERE estado IN ('PENDIENTE', 'ERROR');

CREATE INDEX idx_envio_email_estado_creado
    ON envio_email (estado, created_at);
