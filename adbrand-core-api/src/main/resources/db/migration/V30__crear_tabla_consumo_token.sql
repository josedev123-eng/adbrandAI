-- HU 17: tokens de IA que consume cada empresa en cada generación (panel del administrador técnico).
-- La empresa es el usuario_id que pidió la generación. Sin FK a usuario: esa tabla llega con la HU 21.
CREATE TABLE consumo_token (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    origen VARCHAR(30) NOT NULL CHECK (origen IN ('ANUNCIO', 'REGENERAR_ANUNCIO', 'KIT_MARCA')),
    modelo VARCHAR(120),
    tokens_entrada INT NOT NULL DEFAULT 0,
    tokens_salida INT NOT NULL DEFAULT 0,
    tokens_total INT NOT NULL CHECK (tokens_total >= 0),
    simulado BOOLEAN NOT NULL DEFAULT FALSE,
    fecha TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_consumo_token_usuario_fecha ON consumo_token (usuario_id, fecha);
CREATE INDEX idx_consumo_token_fecha ON consumo_token (fecha);
