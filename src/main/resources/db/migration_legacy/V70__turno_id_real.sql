ALTER TABLE turno ADD COLUMN id BIGINT;

UPDATE turno SET id = 1 WHERE codigo = 'T1';
UPDATE turno SET id = 2 WHERE codigo = 'T2';

ALTER TABLE turno ALTER COLUMN id SET NOT NULL;
ALTER TABLE turno ADD CONSTRAINT uk_turno_id UNIQUE (id);

CREATE SEQUENCE turno_id_seq START WITH 3 OWNED BY turno.id;
ALTER TABLE turno ALTER COLUMN id SET DEFAULT nextval('turno_id_seq');
