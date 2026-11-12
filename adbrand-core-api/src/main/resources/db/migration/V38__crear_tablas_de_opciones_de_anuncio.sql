-- Generar 4 opciones de anuncio y elegir una (idea del profesor para el flujo "Generar → Elegir → Retocar").
-- generacion_anuncio: cada vez que la PYME pide opciones para una oferta.
CREATE TABLE generacion_anuncio (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL REFERENCES usuario(id),
    oferta VARCHAR(300) NOT NULL,
    red_social VARCHAR(20) NOT NULL CHECK (red_social IN ('INSTAGRAM', 'FACEBOOK')),
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_generacion_anuncio_usuario ON generacion_anuncio (usuario_id, fecha_creacion);

-- opcion_anuncio: las 4 opciones de una generación, cada una con su texto (IA de texto) y su imagen (IA de imágenes).
-- La imagen se guarda en base64 con su tipo (image/jpeg, image/png o image/svg+xml si es de prueba).
-- Al elegir una opción se crea su contenido (pasa por el filtro de la HU 13) y queda enlazado en contenido_id.
CREATE TABLE opcion_anuncio (
    id BIGSERIAL PRIMARY KEY,
    generacion_id BIGINT NOT NULL REFERENCES generacion_anuncio(id) ON DELETE CASCADE,
    numero INT NOT NULL CHECK (numero BETWEEN 1 AND 4),
    estilo VARCHAR(40) NOT NULL,
    texto TEXT NOT NULL,
    texto_simulado BOOLEAN NOT NULL DEFAULT FALSE,
    imagen_tipo VARCHAR(40) NOT NULL,
    imagen_base64 TEXT NOT NULL,
    proveedor_imagen VARCHAR(20) NOT NULL,
    imagen_simulada BOOLEAN NOT NULL DEFAULT FALSE,
    elegida BOOLEAN NOT NULL DEFAULT FALSE,
    contenido_id BIGINT REFERENCES contenido(id),
    UNIQUE (generacion_id, numero)
);
