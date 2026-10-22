-- HU 27: operaciones de pago con Yape. El pago se guarda en la tabla pago recién al confirmar.
CREATE TABLE operacion_yape (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL REFERENCES plan(id),
    monto NUMERIC(10,2) NOT NULL CHECK (monto > 0),
    codigo VARCHAR(20) NOT NULL UNIQUE,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'CONFIRMADO', 'ANULADO')),
    celular VARCHAR(9),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_confirmacion TIMESTAMP
);

CREATE INDEX idx_operacion_yape_usuario ON operacion_yape (usuario_id);
CREATE INDEX idx_operacion_yape_estado ON operacion_yape (estado);
