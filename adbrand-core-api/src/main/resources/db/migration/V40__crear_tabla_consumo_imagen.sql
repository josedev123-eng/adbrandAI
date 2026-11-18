-- Control de gasto de las IA de imágenes (mismo enfoque que consumo_token de la HU 17).
-- Cada imagen generada queda anotada: quién la pidió, qué IA la hizo y si fue un borrador (gratis) o un retoque (de pago).
-- Nota: V40 queda fuera del rango V30-V39 de Jose (ya usado completo): hay que avisarlo al equipo.
CREATE TABLE consumo_imagen (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('BORRADOR', 'RETOQUE')),
    proveedor VARCHAR(20) NOT NULL,
    simulado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consumo_imagen_usuario_fecha ON consumo_imagen (usuario_id, fecha);
CREATE INDEX idx_consumo_imagen_fecha ON consumo_imagen (fecha);
