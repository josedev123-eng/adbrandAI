-- HU 03: agregar columna de estado a la tabla de administradores.
ALTER TABLE usuario_admin ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO'));

COMMENT ON COLUMN usuario_admin.estado IS 'Estado de la cuenta: ACTIVO o INACTIVO (HU 03)';
