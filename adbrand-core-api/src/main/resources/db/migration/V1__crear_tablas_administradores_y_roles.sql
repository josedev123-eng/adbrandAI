-- HU-01: tablas de administradores y roles

CREATE TABLE rol (
    id              SERIAL       PRIMARY KEY,
    nombre          VARCHAR(50)  NOT NULL UNIQUE,
    descripcion     VARCHAR(200),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario_admin (
    id              BIGSERIAL    PRIMARY KEY,
    nombres         VARCHAR(100) NOT NULL,
    apellidos       VARCHAR(100) NOT NULL,
    correo          VARCHAR(150) NOT NULL UNIQUE,
    contrasena      VARCHAR(255) NOT NULL,
    rol_id          INTEGER      NOT NULL REFERENCES rol(id),
    fecha_creacion  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO rol (nombre, descripcion) VALUES
    ('SUPERADMIN', 'Acceso total. Es el único que crea cuentas de administrador'),
    ('MODERADOR',  'Revisa y aprueba o rechaza el contenido generado'),
    ('FINANZAS',   'Consulta suscripciones e ingresos');