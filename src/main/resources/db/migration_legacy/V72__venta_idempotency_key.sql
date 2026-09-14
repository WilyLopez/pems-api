ALTER TABLE venta
    ADD COLUMN idempotency_key TEXT;

CREATE UNIQUE INDEX uk_venta_created_by_idempotency_key
    ON venta (created_by, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
