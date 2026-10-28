-- HU 29: el logo que el dueño diseña en el editor interactivo (texto, forma, colores y tipografía).
-- Se guarda como datos del diseño en JSON, no como imagen ni código: la web dibuja el logo a partir de ellos.
-- kit_marca queda con el último diseño y kit_marca_version (V17) guarda cada versión, igual que en la HU 32.
ALTER TABLE kit_marca ADD COLUMN logo_diseno TEXT;
ALTER TABLE kit_marca_version ADD COLUMN logo_diseno TEXT;
