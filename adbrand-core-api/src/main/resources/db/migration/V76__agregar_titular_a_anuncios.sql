-- HU 67: el titular corto del anuncio (3 a 6 palabras) que escribe la IA aparte del texto.
-- Se usa en la imagen, el afiche y el video. Null en los anuncios anteriores (ahí se usa la primera frase del texto).
ALTER TABLE opcion_anuncio ADD COLUMN titular VARCHAR(80);
ALTER TABLE contenido ADD COLUMN titular VARCHAR(80);
