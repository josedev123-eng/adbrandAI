-- Cuando un moderador aprueba o rechaza un contenido que estaba en revisión (HU 15, Django), el dueño
-- tiene que enterarse: aviso_pendiente queda en TRUE hasta que abre "Mis anuncios" en la web.
ALTER TABLE contenido ADD COLUMN aviso_pendiente BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_contenido_usuario_aviso ON contenido (usuario_id) WHERE aviso_pendiente;
