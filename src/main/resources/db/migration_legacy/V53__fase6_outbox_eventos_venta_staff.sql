DROP TRIGGER IF EXISTS trg_evento_notificar_solicitud ON evento;

UPDATE tipo_notificacion
SET plantilla_titulo  = '¡Evento confirmado!',
    plantilla_mensaje = 'Tu evento privado del {fecha} fue confirmado. Nos vemos pronto.'
WHERE codigo = 'EVENTO_CONFIRMADO'
  AND plantilla_titulo IS NULL;

INSERT INTO tipo_notificacion
    (codigo, modulo, nombre, descripcion, destinatario_default, canales_default,
     plantilla_titulo, plantilla_mensaje, prioridad, es_sistema, es_obligatoria, orden)
VALUES
    ('EVENTO_ABONO_RECIBIDO',
     'evento', 'Abono recibido', 'Se registró un abono al evento privado del cliente',
     'CLIENTE', ARRAY['IN_APP','EMAIL'],
     'Abono recibido — S/ {monto}',
     'Registramos tu abono de S/ {monto} para el evento del {fecha}. Saldo pendiente: S/ {saldo}.',
     'NORMAL', TRUE, FALSE, 175),

    ('USUARIO_ACTIVACION',
     'sistema', 'Activación de cuenta de staff', 'Enlace de activación para un usuario de staff recién creado',
     'ADMIN', ARRAY['EMAIL'],
     'Activa tu cuenta — Kiki y Lala',
     'Se creó tu cuenta de staff. Ingresa al enlace de activación para establecer tu contraseña.',
     'ALTA', TRUE, TRUE, 144);

CREATE TABLE staff_token (
    id                  BIGINT       GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    usuario_id          UUID         NOT NULL REFERENCES perfil_usuario(id) ON DELETE CASCADE,
    token_hash          TEXT         NOT NULL UNIQUE,
    tipo                TEXT         NOT NULL,
    expira_at           TIMESTAMPTZ  NOT NULL,
    usado_at            TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_staff_token_tipo CHECK (tipo IN ('ACTIVACION_CUENTA'))
);

CREATE INDEX idx_staff_token_usuario ON staff_token (usuario_id);
CREATE INDEX idx_staff_token_expira  ON staff_token (expira_at) WHERE usado_at IS NULL;

ALTER TABLE staff_token ENABLE ROW LEVEL SECURITY;

CREATE POLICY staff_token_propio ON staff_token FOR ALL TO authenticated USING (
    usuario_id = auth.uid() OR app.es_staff()
) WITH CHECK (
    usuario_id = auth.uid() OR app.es_staff()
);
