-- HU 31: la identidad visual (tipografías y voz de marca) se edita y queda como versión del kit.
-- kit_marca_version (V17) guardaba las tipografías pero no la voz; con esto el historial guarda la identidad completa.
ALTER TABLE kit_marca_version ADD COLUMN voz_marca TEXT;
