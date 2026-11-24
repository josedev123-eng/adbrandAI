-- Recuperar la contraseña de un administrador (panel admin, Django).
-- El código de 6 dígitos se guarda cifrado; vence a los 15 minutos, sirve una sola vez
-- y se bloquea después de 5 intentos fallidos.
CREATE TABLE codigo_recuperacion_admin (
    id             BIGSERIAL PRIMARY KEY,
    admin_id       BIGINT       NOT NULL REFERENCES usuario_admin (id) ON DELETE CASCADE,
    codigo_hash    VARCHAR(255) NOT NULL,
    expira_en      TIMESTAMPTZ  NOT NULL,
    usado          BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos       INTEGER      NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_codigo_recuperacion_admin_admin ON codigo_recuperacion_admin (admin_id);
