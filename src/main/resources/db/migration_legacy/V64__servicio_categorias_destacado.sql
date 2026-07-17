ALTER TABLE servicio_cotizacion
    ADD COLUMN es_destacado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE categoria_servicio (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    nombre       TEXT NOT NULL,
    orden        INT NOT NULL DEFAULT 0,
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_categoria_servicio_nombre ON categoria_servicio (nombre);

CREATE TRIGGER trg_categoria_servicio_updated_at
    BEFORE UPDATE ON categoria_servicio
    FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();

ALTER TABLE servicio_cotizacion
    ADD COLUMN categoria_id BIGINT REFERENCES categoria_servicio(id) ON DELETE SET NULL;

CREATE INDEX idx_servicio_cotizacion_categoria ON servicio_cotizacion (categoria_id) WHERE categoria_id IS NOT NULL;

ALTER TABLE categoria_servicio ENABLE ROW LEVEL SECURITY;

CREATE POLICY categoria_servicio_read  ON categoria_servicio FOR SELECT TO anon, authenticated USING (activo = TRUE OR app.es_staff());
CREATE POLICY categoria_servicio_admin ON categoria_servicio FOR ALL    TO authenticated USING (app.usuario_tiene_permiso('servicio.gestionar')) WITH CHECK (app.usuario_tiene_permiso('servicio.gestionar'));
