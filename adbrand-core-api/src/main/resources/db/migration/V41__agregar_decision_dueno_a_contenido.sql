-- HU 53 y 54: el dueño aprueba o rechaza desde el móvil el contenido generado.
-- Es distinto de "estado", que es lo que decidió el filtro automático o el moderador (HU 13 a 15).
-- PENDIENTE = todavía está en la lista "por revisar" del dueño.
ALTER TABLE contenido
    ADD COLUMN decision_dueno VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE'
        CHECK (decision_dueno IN ('PENDIENTE', 'APROBADO', 'RECHAZADO')),
    ADD COLUMN fecha_decision TIMESTAMP;

CREATE INDEX idx_contenido_usuario_decision ON contenido (usuario_id, decision_dueno);
