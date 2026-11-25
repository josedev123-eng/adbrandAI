-- HU 52: aviso de renovación. Guarda si la suscripción se renueva sola o si la
-- PYME debe pagar manualmente antes del vencimiento. Las suscripciones que ya
-- existen quedan en renovación automática para no cambiar su comportamiento.
ALTER TABLE suscripcion
    ADD COLUMN renovacion_automatica BOOLEAN NOT NULL DEFAULT TRUE;
