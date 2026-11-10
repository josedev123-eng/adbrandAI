-- HU 35: campañas y eventos especiales de la PYME (por ejemplo "Navidad 2026").
-- Cada campaña tiene un color para pintar sus publicaciones en el calendario.
CREATE TABLE campana (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    nombre VARCHAR(100) NOT NULL,
    color VARCHAR(7) NOT NULL CHECK (color ~ '^#[0-9A-F]{6}$'),
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('CAMPANA', 'EVENTO')),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (fecha_fin >= fecha_inicio)
);

CREATE INDEX idx_campana_usuario ON campana (usuario_id);

-- Una publicación del calendario puede pertenecer a una campaña (o a ninguna).
ALTER TABLE publicacion_programada ADD COLUMN campana_id BIGINT REFERENCES campana(id);

CREATE INDEX idx_publicacion_campana ON publicacion_programada (campana_id);
