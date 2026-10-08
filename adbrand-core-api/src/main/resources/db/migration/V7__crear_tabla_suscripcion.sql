-- HU 08: tabla de suscripciones de clientes.
CREATE TABLE suscripcion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    cliente VARCHAR(150) NOT NULL,
    plan VARCHAR(50) NOT NULL,
    monto NUMERIC(10,2) NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('ACTIVA', 'VENCIDA', 'PENDIENTE_PAGO', 'CANCELADA')),
    fecha_inicio DATE NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_suscripcion_estado ON suscripcion (estado);
CREATE INDEX idx_suscripcion_vencimiento ON suscripcion (fecha_vencimiento);
