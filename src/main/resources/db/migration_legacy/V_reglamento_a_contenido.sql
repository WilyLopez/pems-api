INSERT INTO seccion_web (codigo, nombre, descripcion, orden, es_sistema)
VALUES ('ZONA_JUEGOS', 'Zona de juegos', 'Contenido de la zona de juegos', 10, TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO contenido_web (seccion_codigo, tipo_contenido_codigo, clave, valor_es, es_visible, version, orden, metadatos)
VALUES
    ('ZONA_JUEGOS', 'TEXTO', 'zona.reglamento.titulo',
        'Reglamento del local', TRUE, 1, 0, '{}'),
    ('ZONA_JUEGOS', 'TEXTO', 'zona.reglamento.subtitulo',
        'Para garantizar la seguridad y diversión de todos los niños', TRUE, 1, 1, '{}'),
    ('ZONA_JUEGOS', 'TEXTO', 'zona.reglamento.items',
        '["Niños de 1 a 12 años","Calcetines obligatorios para todos","Prohibido ingresar con comida externa","Adultos deben permanecer en el local","Sin objetos punzantes o peligrosos","Respetar el aforo por zona"]',
        TRUE, 1, 2, '{}')
ON CONFLICT (seccion_codigo, clave) DO NOTHING;
