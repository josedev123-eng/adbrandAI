-- Contraseña temporal para administradores nuevos: el superadministrador crea la cuenta con una
-- contraseña temporal y el administrador debe cambiarla en su primer inicio de sesión.
-- Las cuentas que ya existían no tienen que cambiarla.
ALTER TABLE usuario_admin
    ADD COLUMN debe_cambiar_contrasena BOOLEAN NOT NULL DEFAULT FALSE;
