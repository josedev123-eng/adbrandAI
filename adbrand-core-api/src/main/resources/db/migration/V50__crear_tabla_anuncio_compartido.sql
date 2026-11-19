-- HU 47: cada vez que la empresa comparte un anuncio desde la app (WhatsApp, correo, etc.) queda anotado cuándo y en qué formato.
-- Nota: V50 queda fuera del rango V40-V49 de Jose (ya usado completo). Edu usa V51-V53 para sus migraciones del Sprint 4.
CREATE TABLE anuncio_compartido (
    id BIGSERIAL PRIMARY KEY,
    contenido_id BIGINT NOT NULL REFERENCES contenido(id) ON DELETE CASCADE,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    formato VARCHAR(20) NOT NULL CHECK (formato IN ('CUADRADO', 'VERTICAL', 'HORIZONTAL')),
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_anuncio_compartido_contenido ON anuncio_compartido (contenido_id);
CREATE INDEX idx_anuncio_compartido_usuario_fecha ON anuncio_compartido (usuario_id, fecha);
