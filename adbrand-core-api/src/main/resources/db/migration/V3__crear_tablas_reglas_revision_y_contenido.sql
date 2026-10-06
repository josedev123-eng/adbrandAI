-- HU 13: palabras y frases que el filtro automático busca en el texto generado.
CREATE TABLE regla_revision (
    id BIGSERIAL PRIMARY KEY,
    termino VARCHAR(100) NOT NULL UNIQUE,
    categoria VARCHAR(30) NOT NULL CHECK (categoria IN ('LENGUAJE_OFENSIVO', 'PUBLICIDAD_ENGANOSA', 'PRODUCTO_PROHIBIDO')),
    motivo VARCHAR(200) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- HU 13: todo lo que genera la IA se guarda aquí con el resultado de la revisión.
-- APROBADO = pasó el filtro y se puede publicar; DUDOSO = va a la bandeja (HU 14); RECHAZADO = lo decide el moderador (HU 15).
CREATE TABLE contenido (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL CHECK (tipo IN ('ANUNCIO')),
    red_social VARCHAR(20) NOT NULL,
    tono VARCHAR(20) NOT NULL,
    oferta VARCHAR(300) NOT NULL,
    texto TEXT NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('APROBADO', 'DUDOSO', 'RECHAZADO')),
    motivo_revision VARCHAR(500),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_contenido_estado ON contenido (estado);

-- Reglas iniciales. El administrador podrá agregar más después.
INSERT INTO regla_revision (termino, categoria, motivo) VALUES
    ('idiota', 'LENGUAJE_OFENSIVO', 'Contiene un insulto.'),
    ('estúpido', 'LENGUAJE_OFENSIVO', 'Contiene un insulto.'),
    ('imbécil', 'LENGUAJE_OFENSIVO', 'Contiene un insulto.'),
    ('cura el cáncer', 'PUBLICIDAD_ENGANOSA', 'Promete una cura médica.'),
    ('100% garantizado', 'PUBLICIDAD_ENGANOSA', 'Promete un resultado garantizado.'),
    ('sin ningún riesgo', 'PUBLICIDAD_ENGANOSA', 'Promete que no hay riesgo.'),
    ('baja de peso sin esfuerzo', 'PUBLICIDAD_ENGANOSA', 'Promesa de salud sin respaldo.'),
    ('armas de fuego', 'PRODUCTO_PROHIBIDO', 'Ofrece un producto prohibido.'),
    ('cigarrillos', 'PRODUCTO_PROHIBIDO', 'Publicidad de tabaco.'),
    ('apuestas', 'PRODUCTO_PROHIBIDO', 'Publicidad de apuestas.');