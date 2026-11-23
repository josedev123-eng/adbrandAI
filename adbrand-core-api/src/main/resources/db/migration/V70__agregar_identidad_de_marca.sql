-- Identidad de marca: lo que la empresa quiere transmitir, su eslogan y su logo como imagen.
-- sentimiento: qué quiere que sienta su cliente (confianza, alegría…). diferencial: qué la hace distinta.
-- eslogan: el elegido (o el que ya tenía). eslogan_opciones: las 3 propuestas de la IA, una por línea.
-- logo_imagen: el logo en base64 (subido por la empresa o elegido entre los que hizo la IA), con su tipo.
-- logo_origen: SUBIDO o IA. Los kits que ya existían quedan como estaban (todo null).
ALTER TABLE kit_marca
    ADD COLUMN sentimiento VARCHAR(100),
    ADD COLUMN diferencial VARCHAR(300),
    ADD COLUMN eslogan VARCHAR(150),
    ADD COLUMN eslogan_opciones TEXT,
    ADD COLUMN logo_imagen TEXT,
    ADD COLUMN logo_imagen_tipo VARCHAR(20),
    ADD COLUMN logo_origen VARCHAR(10);
