-- =====================================================================
-- FASE 1 — Sesiones de Caja (por usuario)
-- Ejecutar manualmente en Supabase ANTES de levantar el backend.
-- Reemplaza el modelo "una caja por sede/fecha" (apertura_caja) por
-- sesiones de caja por usuario (CAJERO / ADMINISTRATIVA).
-- El enrutamiento del efectivo pasa a la capa de aplicacion: se retiran
-- los triggers que insertaban en movimiento_caja.
-- =====================================================================

CREATE TABLE sesion_caja (
    id              BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sede_id         BIGINT       NOT NULL REFERENCES sede(id) ON DELETE RESTRICT,
    usuario_id      UUID         NOT NULL REFERENCES perfil_usuario(id),
    tipo            TEXT         NOT NULL CHECK (tipo IN ('CAJERO','ADMINISTRATIVA')),
    estado_codigo   TEXT         NOT NULL DEFAULT 'ABIERTA' REFERENCES estado_caja(codigo) ON UPDATE CASCADE,

    saldo_inicial   NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_ingresos  NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_egresos   NUMERIC(10,2) NOT NULL DEFAULT 0,
    saldo_esperado  NUMERIC(10,2),
    saldo_final     NUMERIC(10,2),
    diferencia      NUMERIC(10,2),

    abierta_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    cerrada_at      TIMESTAMPTZ,
    cerrada_por     UUID         REFERENCES perfil_usuario(id),
    motivo_cierre   TEXT,
    observaciones   TEXT,

    version         BIGINT       NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_sesion_caja_saldo_inicial CHECK (saldo_inicial >= 0),
    CONSTRAINT ck_sesion_caja_saldo_final   CHECK (saldo_final IS NULL OR saldo_final >= 0),
    CONSTRAINT ck_sesion_caja_totales       CHECK (total_ingresos >= 0 AND total_egresos >= 0),
    CONSTRAINT ck_sesion_caja_cierre        CHECK (
        (estado_codigo = 'ABIERTA' AND cerrada_at IS NULL)
        OR (estado_codigo = 'CERRADA' AND cerrada_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_sesion_caja_usuario_abierta
    ON sesion_caja (usuario_id) WHERE estado_codigo = 'ABIERTA';
CREATE INDEX idx_sesion_caja_sede_estado ON sesion_caja (sede_id, estado_codigo);
CREATE INDEX idx_sesion_caja_sede_abierta ON sesion_caja (sede_id, abierta_at);
CREATE INDEX idx_sesion_caja_usuario ON sesion_caja (usuario_id, abierta_at);

CREATE TRIGGER trg_sesion_caja_updated_at
    BEFORE UPDATE ON sesion_caja
    FOR EACH ROW EXECUTE FUNCTION app.set_updated_at();


-- ---------------------------------------------------------------------
-- Repunte de movimiento_caja: de apertura_caja_id a sesion_caja_id.
-- ---------------------------------------------------------------------
ALTER TABLE movimiento_caja
    ADD COLUMN sesion_caja_id BIGINT REFERENCES sesion_caja(id) ON DELETE RESTRICT;

ALTER TABLE movimiento_caja ALTER COLUMN apertura_caja_id DROP NOT NULL;

CREATE INDEX idx_movimiento_caja_sesion ON movimiento_caja (sesion_caja_id, created_at);


-- ---------------------------------------------------------------------
-- Repunte de arqueo_caja: de apertura_caja_id a sesion_caja_id.
-- ---------------------------------------------------------------------
ALTER TABLE arqueo_caja
    ADD COLUMN sesion_caja_id BIGINT REFERENCES sesion_caja(id) ON DELETE RESTRICT;

ALTER TABLE arqueo_caja ALTER COLUMN apertura_caja_id DROP NOT NULL;

CREATE INDEX idx_arqueo_caja_sesion ON arqueo_caja (sesion_caja_id);


-- ---------------------------------------------------------------------
-- Enrutamiento del efectivo pasa a la capa de aplicacion.
-- Se retiran los triggers que insertaban en movimiento_caja.
-- El trigger trg_venta_pago_registrar_ingreso se conserva: registro_ingreso
-- sigue siendo la verdad contable.
-- ---------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_registro_ingreso_movimiento_caja ON registro_ingreso;
DROP TRIGGER IF EXISTS trg_registro_egreso_movimiento_caja  ON registro_egreso;

DROP FUNCTION IF EXISTS app.registrar_movimiento_caja_desde_ingreso();
DROP FUNCTION IF EXISTS app.registrar_movimiento_caja_desde_egreso();

-- Nota: la tabla apertura_caja se deja en la base como legado inactivo.
-- Su DROP se realizara al consolidar todas las fases del rediseno.
