CREATE UNIQUE INDEX uk_sesion_caja_sede_tipo_abierta
    ON sesion_caja (sede_id, tipo)
    WHERE estado_codigo = 'ABIERTA';
