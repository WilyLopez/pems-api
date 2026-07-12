ALTER TABLE contrato_evento
    ADD COLUMN cargado_por UUID REFERENCES perfil_usuario(id);

ALTER TABLE contrato_evento
    RENAME COLUMN firmado_at TO cargado_at;

UPDATE contrato_evento
SET cargado_por = redactor_id
WHERE cargado_por IS NULL
  AND redactor_id IS NOT NULL;

DELETE FROM contrato_evento
WHERE archivo_pdf_path IS NULL;

ALTER TABLE contrato_evento
    DROP CONSTRAINT contrato_evento_estado_check,
    DROP COLUMN estado,
    DROP COLUMN contenido_texto,
    DROP COLUMN plantilla,
    DROP COLUMN observaciones,
    DROP COLUMN redactor_id,
    DROP COLUMN version_v;

ALTER TABLE contrato_evento
    ALTER COLUMN archivo_pdf_path SET NOT NULL,
    ALTER COLUMN cargado_at SET NOT NULL,
    ALTER COLUMN cargado_at SET DEFAULT NOW(),
    ALTER COLUMN cargado_por SET NOT NULL;

DROP TABLE contrato_documento;

DROP TABLE contrato;

DROP TABLE estado_contrato;

UPDATE tipo_notificacion
SET activo = FALSE
WHERE codigo IN ('CONTRATO_FIRMADO', 'CONTRATO_VENCIDO_SIN_FIRMA');

UPDATE tipo_notificacion
SET nombre = 'Contrato disponible',
    descripcion = 'El administrador cargo el contrato en PDF',
    plantilla_titulo = 'Contrato disponible — {evento}',
    plantilla_mensaje = 'Tu contrato para el evento del {fecha} ya esta disponible. Ingresa a tu portal para descargarlo.'
WHERE codigo = 'EVENTO_CONTRATO_LISTO';

DROP FUNCTION IF EXISTS app.actualizar_estado_contrato_pendiente();
