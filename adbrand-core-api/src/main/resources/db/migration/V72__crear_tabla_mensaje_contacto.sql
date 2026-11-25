-- "Contacto" del pie de página: el formulario de soporte. Cada mensaje queda guardado (el administrador los ve y
-- los marca como atendidos) y además se avisa por correo al equipo. usuario_id es null si se escribió sin sesión.
CREATE TABLE mensaje_contacto (
    id             BIGSERIAL PRIMARY KEY,
    usuario_id     BIGINT REFERENCES usuario (id) ON DELETE SET NULL,
    nombre         VARCHAR(100) NOT NULL,
    correo         VARCHAR(150) NOT NULL,
    asunto         VARCHAR(40)  NOT NULL CHECK (asunto IN ('DUDA', 'PROBLEMA', 'PAGOS', 'SUGERENCIA', 'OTRO')),
    mensaje        TEXT         NOT NULL,
    atendido       BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_creacion TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mensaje_contacto_fecha ON mensaje_contacto (atendido, fecha_creacion DESC);
