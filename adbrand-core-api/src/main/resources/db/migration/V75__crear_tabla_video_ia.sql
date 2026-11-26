-- HU 66: videos con IA (OpenRouter). Cada pedido guarda su trabajo en OpenRouter, en qué va, cuánto costó y el MP4
-- final (con el titular y la marca encima). El consumo también se anota en consumo_imagen (tipo VIDEO) con su costo,
-- para verlo en "Consumo de IA" del panel de administración.
CREATE TABLE video_ia (
    id                BIGSERIAL PRIMARY KEY,
    usuario_id        BIGINT       NOT NULL REFERENCES usuario (id),
    contenido_id      BIGINT       NOT NULL REFERENCES contenido (id),
    trabajo_id        VARCHAR(100),
    modelo            VARCHAR(100) NOT NULL,
    estado            VARCHAR(20)  NOT NULL CHECK (estado IN ('CREANDO', 'LISTO', 'FALLIDO')),
    progreso          INTEGER,
    costo             NUMERIC(10, 4),
    error             VARCHAR(300),
    video             BYTEA,
    fecha_creacion    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_fin         TIMESTAMP
);

CREATE INDEX idx_video_ia_usuario_fecha ON video_ia (usuario_id, fecha_creacion);

ALTER TABLE consumo_imagen DROP CONSTRAINT IF EXISTS consumo_imagen_tipo_check;
ALTER TABLE consumo_imagen ADD CONSTRAINT consumo_imagen_tipo_check CHECK (tipo IN ('BORRADOR', 'RETOQUE', 'VIDEO'));
ALTER TABLE consumo_imagen ADD COLUMN costo NUMERIC(10, 4);
