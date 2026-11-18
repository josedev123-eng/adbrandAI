-- "Retocar con prompts cortos": cada retoque de la imagen de la opción elegida queda como una versión (v1, v2, v3…).
-- v1 es la imagen original de la opción (instruccion NULL). Las siguientes guardan la instrucción que la cambió.
CREATE TABLE version_anuncio (
    id BIGSERIAL PRIMARY KEY,
    opcion_id BIGINT NOT NULL REFERENCES opcion_anuncio(id) ON DELETE CASCADE,
    numero INT NOT NULL CHECK (numero >= 1),
    instruccion VARCHAR(300),
    imagen_tipo VARCHAR(40) NOT NULL,
    imagen_base64 TEXT NOT NULL,
    proveedor_imagen VARCHAR(20) NOT NULL,
    imagen_simulada BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (opcion_id, numero)
);

-- Qué versión se está usando ahora en la opción (para "deshacer" volviendo a una anterior). NULL = la original.
ALTER TABLE opcion_anuncio ADD COLUMN version_actual INT;
