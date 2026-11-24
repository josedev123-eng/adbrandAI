-- Color del avatar (círculo con las iniciales) que el usuario elige en "Mi cuenta".
-- NULL = todavía no eligió: la web asigna uno automático según el nombre de la empresa.
ALTER TABLE usuario
    ADD COLUMN color_avatar VARCHAR(20)
        CHECK (color_avatar IN ('INDIGO', 'CORAL', 'VERDE', 'AZUL', 'VIOLETA', 'ROSA'));
