-- Avatar con imagen (Mi Cuenta): el logo del kit o una foto que sube la empresa. Se guarda ya reducida a 256 px
-- como data URL (PNG), así pesa poco. null = avatar de iniciales con su color (color_avatar, V42).
ALTER TABLE usuario ADD COLUMN avatar_imagen TEXT;
