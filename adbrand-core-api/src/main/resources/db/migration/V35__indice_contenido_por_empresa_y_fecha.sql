-- HU 37: el informe consolidado de campañas lee los anuncios de una empresa ordenados por fecha.
-- La tabla contenido (V3) solo tenía índice por estado; con este, la consulta por empresa y fecha no recorre toda la tabla.
CREATE INDEX idx_contenido_usuario_fecha ON contenido (usuario_id, fecha_creacion DESC);
