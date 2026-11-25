-- WhatsApp (Estados) como red social al crear un anuncio, además de Instagram y Facebook.
-- La tabla contenido guarda la red sin restricción; las 4 opciones de anuncio sí tenían una lista fija.
ALTER TABLE generacion_anuncio DROP CONSTRAINT IF EXISTS generacion_anuncio_red_social_check;
ALTER TABLE generacion_anuncio
    ADD CONSTRAINT generacion_anuncio_red_social_check CHECK (red_social IN ('INSTAGRAM', 'FACEBOOK', 'WHATSAPP'));
