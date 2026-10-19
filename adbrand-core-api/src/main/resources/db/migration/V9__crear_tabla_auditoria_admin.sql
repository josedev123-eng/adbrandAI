-- HU 04: registro de las acciones que realiza cada administrador.
CREATE TABLE auditoria_admin (
    id BIGSERIAL PRIMARY KEY,
    admin_id BIGINT NOT NULL REFERENCES usuario_admin(id),
    accion VARCHAR(50) NOT NULL,
    entidad VARCHAR(50) NOT NULL,
    entidad_id BIGINT,
    detalle TEXT,
    fecha TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auditoria_admin_admin ON auditoria_admin (admin_id);
CREATE INDEX idx_auditoria_admin_fecha ON auditoria_admin (fecha);
