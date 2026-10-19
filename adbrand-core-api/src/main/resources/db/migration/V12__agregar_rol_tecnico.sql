INSERT INTO rol (nombre, descripcion) VALUES
    ('TECNICO', 'Monitorea el consumo de tokens de IA y el estado del servidor')
ON CONFLICT (nombre) DO NOTHING;