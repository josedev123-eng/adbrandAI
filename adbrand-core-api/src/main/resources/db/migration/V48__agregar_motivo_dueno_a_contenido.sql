-- HU 54: cuando el dueño rechaza un contenido desde el móvil puede dejar un motivo (opcional),
-- por ejemplo "el precio está mal". Distinto de motivo_revision, que es el del moderador.
ALTER TABLE contenido
    ADD COLUMN motivo_dueno VARCHAR(300);
