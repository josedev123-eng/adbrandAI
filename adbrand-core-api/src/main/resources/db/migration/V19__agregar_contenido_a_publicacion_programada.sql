-- HU 34: una publicación programada puede llevar uno de los anuncios que generó la IA (tabla contenido, V3).
-- La hora, la red social y el estado ya existen desde V24. Las publicaciones de la HU 33 quedan sin anuncio (NULL).
ALTER TABLE publicacion_programada ADD COLUMN contenido_id BIGINT REFERENCES contenido(id);

CREATE INDEX idx_publicacion_contenido ON publicacion_programada (contenido_id);
