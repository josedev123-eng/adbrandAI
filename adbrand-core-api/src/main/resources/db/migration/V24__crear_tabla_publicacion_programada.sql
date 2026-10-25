-- HU 33: publicaciones que la PYME programa en su calendario mensual.
CREATE TABLE publicacion_programada (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    titulo VARCHAR(150) NOT NULL,
    red_social VARCHAR(20) NOT NULL CHECK (red_social IN ('INSTAGRAM', 'FACEBOOK')),
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADA'
        CHECK (estado IN ('PROGRAMADA', 'PUBLICADA', 'CANCELADA')),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_publicacion_usuario_fecha ON publicacion_programada (usuario_id, fecha);
