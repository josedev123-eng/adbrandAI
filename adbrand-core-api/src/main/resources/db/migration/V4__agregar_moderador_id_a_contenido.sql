-- HU 15: agregar columna para registrar qué moderador aprobó o rechazó el contenido.
ALTER TABLE contenido ADD COLUMN moderador_id BIGINT;

COMMENT ON COLUMN contenido.moderador_id IS 'ID del administrador que aprobó o rechazó el contenido (HU 15)';