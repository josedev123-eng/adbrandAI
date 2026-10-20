-- HU 22: fecha y hora del último inicio de sesión de cada usuario PYME.
ALTER TABLE usuario ADD COLUMN ultimo_acceso TIMESTAMP;
