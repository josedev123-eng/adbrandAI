-- HU-03: agregar columna estado a administradores para activar/desactivar cuentas
ALTER TABLE usuario_admin ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO'));

COMMENT ON COLUMN usuario_admin.estado IS 'Estado de la cuenta del administrador: ACTIVO o INACTIVO (HU 03)';