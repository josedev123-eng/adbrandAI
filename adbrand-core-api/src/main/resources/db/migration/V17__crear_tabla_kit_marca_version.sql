-- HU 32: historial de ediciones del kit de marca. Cada vez que el dueño guarda cambios
-- se agrega una fila con lo que guardó; kit_marca queda siempre con lo último.
CREATE TABLE kit_marca_version (
    id BIGSERIAL PRIMARY KEY,
    kit_marca_id BIGINT NOT NULL REFERENCES kit_marca(id),
    paleta_colores TEXT,
    tipografia_primaria VARCHAR(200),
    tipografia_secundaria VARCHAR(200),
    logo_concepto TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kit_marca_version_kit ON kit_marca_version (kit_marca_id);
