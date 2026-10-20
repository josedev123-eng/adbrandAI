-- HU 24: códigos de 6 dígitos para recuperar la contraseña. Se guarda solo el hash del código.
CREATE TABLE codigo_recuperacion (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    codigo_hash VARCHAR(100) NOT NULL,
    fecha_vencimiento TIMESTAMP NOT NULL,
    usado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_codigo_recuperacion_usuario ON codigo_recuperacion (usuario_id);
