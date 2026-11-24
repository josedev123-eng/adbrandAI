-- HU 51: preferencias de avisos por empresa. Si no hay fila, todo está activo:
-- así las cuentas que ya existían siguen recibiendo sus recordatorios.
CREATE TABLE notification_preferences (
    usuario_id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
    campanas BOOLEAN NOT NULL DEFAULT TRUE,
    suscripciones BOOLEAN NOT NULL DEFAULT TRUE,
    novedades BOOLEAN NOT NULL DEFAULT TRUE,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
