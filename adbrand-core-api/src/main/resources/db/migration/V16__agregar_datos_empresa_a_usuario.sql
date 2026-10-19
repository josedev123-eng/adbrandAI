-- HU 23: datos de la empresa del usuario PYME.
-- Son opcionales porque las cuentas ya creadas no los tienen; se piden al guardar "Mi cuenta".
ALTER TABLE usuario ADD COLUMN ruc VARCHAR(11) UNIQUE;
ALTER TABLE usuario ADD COLUMN razon_social VARCHAR(200);
ALTER TABLE usuario ADD COLUMN telefono VARCHAR(15);
ALTER TABLE usuario ADD COLUMN direccion VARCHAR(250);
