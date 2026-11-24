-- El administrador puede suspender una empresa que abusa (por ejemplo, contenido prohibido repetido).
-- La suspensión usa el estado INACTIVO que ya existía (V13): el login la bloquea. Aquí se guarda el porqué.
ALTER TABLE usuario
    ADD COLUMN motivo_suspension VARCHAR(300),
    ADD COLUMN fecha_suspension TIMESTAMPTZ;
