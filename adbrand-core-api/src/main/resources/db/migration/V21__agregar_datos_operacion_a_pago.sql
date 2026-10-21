-- HU 26: datos de la operación de pago con tarjeta y el plan contratado.
-- Todas las columnas son NULL para no romper los pagos que ya registra Django (HU 6).
ALTER TABLE pago ADD COLUMN usuario_id BIGINT;
ALTER TABLE pago ADD COLUMN plan_id BIGINT REFERENCES plan(id);
ALTER TABLE pago ADD COLUMN estado VARCHAR(20) CHECK (estado IN ('PENDIENTE', 'APROBADO', 'RECHAZADO'));
ALTER TABLE pago ADD COLUMN referencia VARCHAR(40);
ALTER TABLE pago ADD COLUMN titular VARCHAR(120);
ALTER TABLE pago ADD COLUMN ultimos_digitos VARCHAR(4);

CREATE INDEX idx_pago_usuario ON pago (usuario_id);
