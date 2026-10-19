-- HU 19: límite mensual de tokens de IA y desde qué porcentaje se avisa al administrador técnico.
-- usuario_id NULL es el límite por defecto para todas las empresas; una fila con usuario_id es el límite propio
-- de esa empresa. Sin FK a usuario, igual que consumo_token (V30).
CREATE TABLE limite_tokens (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT,
    limite_mensual BIGINT NOT NULL CHECK (limite_mensual > 0),
    porcentaje_aviso INT NOT NULL DEFAULT 80 CHECK (porcentaje_aviso BETWEEN 1 AND 100),
    actualizado TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Una sola fila por empresa y una sola fila por defecto (NULL cuenta como un valor más).
    CONSTRAINT uq_limite_tokens_usuario UNIQUE NULLS NOT DISTINCT (usuario_id)
);

-- Límite por defecto: 100 000 tokens al mes, con aviso al llegar al 80 %.
INSERT INTO limite_tokens (usuario_id, limite_mensual, porcentaje_aviso) VALUES (NULL, 100000, 80);
