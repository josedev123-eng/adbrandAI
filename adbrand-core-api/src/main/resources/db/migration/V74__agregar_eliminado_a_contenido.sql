-- HU 63 (Mis anuncios): el dueño puede eliminar un anuncio que ya canceló en AdBrand. No se borra la fila
-- (los informes, el historial del moderador y las métricas la usan): se marca y deja de verse en sus listas.
ALTER TABLE contenido ADD COLUMN eliminado BOOLEAN NOT NULL DEFAULT FALSE;
