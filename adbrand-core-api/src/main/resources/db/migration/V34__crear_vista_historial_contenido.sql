-- HU 16: historial de los contenidos aprobados o rechazados, con quién lo decidió y por qué.
-- Si moderador_id está vacío, lo decidió el filtro automático de la HU 13.
CREATE VIEW vista_historial_contenido AS
SELECT
    c.id,
    c.usuario_id,
    c.tipo,
    c.red_social,
    c.oferta,
    c.texto,
    c.estado,
    c.motivo_revision AS motivo,
    c.moderador_id,
    COALESCE(u.nombres || ' ' || u.apellidos, 'Filtro automático') AS moderador,
    c.fecha_actualizacion AS fecha_decision,
    c.fecha_creacion
FROM contenido c
LEFT JOIN usuario_admin u ON u.id = c.moderador_id
-- Los DUDOSOS todavía esperan revisión en la bandeja (HU 14), así que no son parte del historial.
WHERE c.estado IN ('APROBADO', 'RECHAZADO');
