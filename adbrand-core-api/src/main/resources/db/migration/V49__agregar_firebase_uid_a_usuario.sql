-- App móvil: inicio de sesión con Firebase Authentication.
-- firebase_uid une la cuenta de Firebase con la empresa de siempre (la misma que usa la web).
-- NULL = la empresa todavía no entró nunca con Firebase.
ALTER TABLE usuario
    ADD COLUMN firebase_uid VARCHAR(128) UNIQUE;
