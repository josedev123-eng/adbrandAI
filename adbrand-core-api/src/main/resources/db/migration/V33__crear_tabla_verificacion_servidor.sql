-- HU 18: cada verificación del estado del servidor (backend, base de datos e IA) que hace el panel del técnico.
CREATE TABLE verificacion_servidor (
    id BIGSERIAL PRIMARY KEY,
    servicio VARCHAR(20) NOT NULL CHECK (servicio IN ('BACKEND', 'BASE_DATOS', 'IA')),
    estado VARCHAR(10) NOT NULL CHECK (estado IN ('ACTIVO', 'CAIDO')),
    tiempo_ms INT,
    -- Por qué está caído (por ejemplo, "tiempo de espera agotado").
    detalle VARCHAR(255),
    fecha TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Para buscar rápido la última verificación de cada servicio.
CREATE INDEX idx_verificacion_servidor_servicio_fecha ON verificacion_servidor (servicio, fecha DESC);
