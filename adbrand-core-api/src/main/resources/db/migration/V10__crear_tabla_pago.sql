-- HU 06: pagos recibidos por cada suscripción. Los ingresos del mes salen de aquí.
CREATE TABLE pago (
    id BIGSERIAL PRIMARY KEY,
    suscripcion_id BIGINT NOT NULL REFERENCES suscripcion(id),
    monto NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    fecha_pago DATE NOT NULL,
    metodo VARCHAR(20) NOT NULL CHECK (metodo IN ('TARJETA', 'YAPE')),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pago_fecha_pago ON pago (fecha_pago);
