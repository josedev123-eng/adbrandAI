-- HU 50: dispositivos de la PYME que reciben notificaciones push.
-- Un mismo teléfono puede iniciar sesión con otra cuenta: el token es único y se
-- reasigna al usuario que lo registró por última vez.
CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    fcm_token VARCHAR(255) NOT NULL UNIQUE,
    platform VARCHAR(10) NOT NULL CHECK (platform IN ('android', 'ios')),
    device_name VARCHAR(120),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Consulta del envío: solo los dispositivos activos de una empresa.
CREATE INDEX idx_user_devices_usuario_activo ON user_devices (user_id) WHERE is_active;
