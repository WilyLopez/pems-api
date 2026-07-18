UPDATE permiso SET es_sistema = FALSE WHERE codigo IN ('usuario.ver', 'usuario.gestionar');

INSERT INTO rol_permiso (rol_codigo, permiso_codigo)
SELECT rol_codigo, 'usuarios.ver'
FROM rol_permiso
WHERE permiso_codigo = 'usuario.ver'
ON CONFLICT DO NOTHING;

DELETE FROM rol_permiso WHERE permiso_codigo = 'usuario.ver';
DELETE FROM permiso WHERE codigo = 'usuario.ver';

UPDATE permiso SET codigo = 'usuarios.gestionar' WHERE codigo = 'usuario.gestionar';
