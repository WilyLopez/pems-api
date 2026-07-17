ALTER TABLE servicio_cotizacion
    ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE TABLE servicio_variante (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id  BIGINT NOT NULL REFERENCES servicio_cotizacion(id) ON DELETE CASCADE,
    nombre       TEXT NOT NULL,
    descripcion  TEXT,
    precio       NUMERIC(10,2) NOT NULL,
    es_activo    BOOLEAN NOT NULL DEFAULT TRUE,
    orden        INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at   TIMESTAMPTZ,
    CONSTRAINT ck_servicio_variante_precio CHECK (precio >= 0)
);

CREATE UNIQUE INDEX uk_servicio_variante_nombre ON servicio_variante (servicio_id, nombre) WHERE deleted_at IS NULL;
CREATE INDEX idx_servicio_variante_servicio ON servicio_variante (servicio_id, orden) WHERE es_activo = TRUE AND deleted_at IS NULL;

CREATE TRIGGER trg_servicio_variante_updated_at
    BEFORE UPDATE ON servicio_variante
    FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();

CREATE TABLE servicio_imagen (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    servicio_id   BIGINT NOT NULL REFERENCES servicio_cotizacion(id) ON DELETE CASCADE,
    variante_id   BIGINT REFERENCES servicio_variante(id) ON DELETE CASCADE,
    archivo_path  TEXT NOT NULL,
    alt_texto     TEXT,
    orden         INT NOT NULL DEFAULT 0,
    es_principal  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_servicio_imagen_servicio ON servicio_imagen (servicio_id, orden);
CREATE UNIQUE INDEX uk_servicio_imagen_principal ON servicio_imagen (servicio_id) WHERE es_principal = TRUE AND variante_id IS NULL;
CREATE UNIQUE INDEX uk_servicio_imagen_principal_variante ON servicio_imagen (variante_id) WHERE es_principal = TRUE AND variante_id IS NOT NULL;

ALTER TABLE evento_servicio
    ADD COLUMN servicio_variante_id BIGINT REFERENCES servicio_variante(id) ON DELETE SET NULL;

CREATE INDEX idx_evento_servicio_variante ON evento_servicio (servicio_variante_id) WHERE servicio_variante_id IS NOT NULL;

ALTER TABLE servicio_variante ENABLE ROW LEVEL SECURITY;
ALTER TABLE servicio_imagen   ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS servicio_read  ON servicio_cotizacion;
DROP POLICY IF EXISTS servicio_admin ON servicio_cotizacion;

CREATE POLICY servicio_read  ON servicio_cotizacion FOR SELECT TO anon, authenticated USING (deleted_at IS NULL AND (es_activo = TRUE OR app.es_staff()));
CREATE POLICY servicio_admin ON servicio_cotizacion FOR ALL    TO authenticated USING (app.usuario_tiene_permiso('servicio.gestionar')) WITH CHECK (app.usuario_tiene_permiso('servicio.gestionar'));

CREATE POLICY svariante_read  ON servicio_variante FOR SELECT TO anon, authenticated USING (deleted_at IS NULL AND (es_activo = TRUE OR app.es_staff()));
CREATE POLICY svariante_admin ON servicio_variante FOR ALL    TO authenticated USING (app.usuario_tiene_permiso('servicio.gestionar')) WITH CHECK (app.usuario_tiene_permiso('servicio.gestionar'));

CREATE POLICY simagen_read  ON servicio_imagen FOR SELECT TO anon, authenticated USING (TRUE);
CREATE POLICY simagen_admin ON servicio_imagen FOR ALL    TO authenticated USING (app.usuario_tiene_permiso('servicio.gestionar')) WITH CHECK (app.usuario_tiene_permiso('servicio.gestionar'));

INSERT INTO permiso (codigo, modulo, nombre, descripcion, orden) VALUES
    ('servicio.ver',        'catalogo', 'Ver servicios',       'Listar servicios de cotización',        76),
    ('servicio.gestionar',  'catalogo', 'Gestionar servicios', 'CRUD servicios, variantes e imágenes',  77);

INSERT INTO rol_permiso (rol_codigo, permiso_codigo) VALUES
    ('SUPERADMIN', 'servicio.ver'),
    ('SUPERADMIN', 'servicio.gestionar'),
    ('ADMIN',      'servicio.ver'),
    ('ADMIN',      'servicio.gestionar'),
    ('CAJERO',     'servicio.ver');
