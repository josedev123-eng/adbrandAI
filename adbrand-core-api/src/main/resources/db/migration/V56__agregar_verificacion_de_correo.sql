-- Verificar el correo al registrarse en la web: se manda un código de 6 dígitos y la cuenta
-- no puede iniciar sesión hasta escribirlo. Las cuentas que ya existían quedan como verificadas.
ALTER TABLE usuario
    ADD COLUMN correo_verificado BOOLEAN NOT NULL DEFAULT TRUE;

-- Solo el hash del código. Vence a los 15 minutos, sirve una vez y se anula tras 5 intentos equivocados.
CREATE TABLE codigo_verificacion_correo (
    id                BIGSERIAL PRIMARY KEY,
    usuario_id        BIGINT       NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    codigo_hash       VARCHAR(100) NOT NULL,
    fecha_vencimiento TIMESTAMP    NOT NULL,
    usado             BOOLEAN      NOT NULL DEFAULT FALSE,
    intentos          INTEGER      NOT NULL DEFAULT 0,
    fecha_creacion    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_codigo_verificacion_correo_usuario ON codigo_verificacion_correo (usuario_id);
