-- Bloqueo por intentos fallidos: tras 5 contraseñas equivocadas seguidas la cuenta queda
-- bloqueada 15 minutos. Aplica a las empresas (web, Spring) y a los administradores (panel, Django).
ALTER TABLE usuario
    ADD COLUMN intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta TIMESTAMP;

ALTER TABLE usuario_admin
    ADD COLUMN intentos_fallidos INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN bloqueado_hasta TIMESTAMPTZ;
