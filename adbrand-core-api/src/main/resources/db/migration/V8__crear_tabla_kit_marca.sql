-- HU 11: tabla de kits de marca generados.
CREATE TABLE kit_marca (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    nombre_negocio VARCHAR(120) NOT NULL,
    sector VARCHAR(80) NOT NULL,
    paleta_sugerida VARCHAR(300),
    valores_eslogan VARCHAR(500),
    estilo_visual VARCHAR(100) NOT NULL,
    logo_concepto TEXT,
    tipografia_primaria VARCHAR(200),
    tipografia_secundaria VARCHAR(200),
    paleta_colores TEXT,
    voz_marca TEXT,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_kit_marca_usuario ON kit_marca (usuario_id);
