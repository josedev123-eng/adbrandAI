-- HU 43: tokens de renovación de la sesión móvil.
-- Se guarda solo el hash SHA-256 del token; el valor original nunca se persiste.
CREATE TABLE token_renovacion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,
    fecha_vencimiento TIMESTAMP NOT NULL,
    revocado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_renovacion_usuario ON token_renovacion(usuario_id);
CREATE INDEX idx_token_renovacion_vigente ON token_renovacion(usuario_id, revocado, fecha_vencimiento);
