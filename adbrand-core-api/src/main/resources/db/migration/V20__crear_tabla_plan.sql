-- HU 25: planes de suscripción que puede contratar la PYME.
-- Los precios y los límites son los del sprint 2; se pueden ajustar con una migración nueva.
CREATE TABLE plan (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(40) NOT NULL UNIQUE,
    precio NUMERIC(10,2) NOT NULL CHECK (precio > 0),
    limite_tokens INTEGER NOT NULL CHECK (limite_tokens > 0),
    descripcion VARCHAR(300) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

INSERT INTO plan (nombre, precio, limite_tokens, descripcion) VALUES
    ('Básico', 29.90, 50000, 'Para empezar: 50 000 tokens de IA al mes y un kit de marca.'),
    ('Pro', 59.90, 150000, 'Para crecer: 150 000 tokens de IA al mes, calendario y reportes.'),
    ('Premium', 99.90, 400000, 'Sin límites prácticos: 400 000 tokens de IA al mes y atención preferente.');
