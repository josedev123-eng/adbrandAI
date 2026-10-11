-- Datos de prueba para el panel financiero (HU 5, 6 y 7) y la lista de deudas (HU 8).
-- Ejecutar MANUALMENTE en la base de datos, una sola vez. No es una migración Flyway.
-- Encoding: UTF-8
--
-- Las fechas se calculan desde el día en que se ejecuta, así que los pagos siempre
-- caen en los últimos 6 meses. Deja 4 suscripciones activas y un mes sin ingresos.

WITH nuevas AS (
    INSERT INTO suscripcion (usuario_id, cliente, plan, monto, estado, fecha_inicio, fecha_vencimiento) VALUES
    (1, 'Panadería Doña Rosa', 'BASICO', 49.90, 'ACTIVA', CURRENT_DATE - 160, CURRENT_DATE + 20),
    (2, 'Café Central', 'PRO', 99.90, 'ACTIVA', CURRENT_DATE - 130, CURRENT_DATE + 15),
    (3, 'Tienda Don Pedro', 'PRO', 99.90, 'ACTIVA', CURRENT_DATE - 100, CURRENT_DATE + 5),
    (4, 'Bodega San Martín', 'BASICO', 49.90, 'ACTIVA', CURRENT_DATE - 40, CURRENT_DATE + 25),
    (5, 'Barbería El Corte', 'BASICO', 49.90, 'ACTIVA', CURRENT_DATE - 70, CURRENT_DATE - 3),
    (6, 'Librería Cerrada', 'BASICO', 49.90, 'VENCIDA', CURRENT_DATE - 150, CURRENT_DATE - 60),
    (7, 'Florería Lima', 'PRO', 99.90, 'PENDIENTE_PAGO', CURRENT_DATE - 12, CURRENT_DATE + 18),
    (8, 'Gimnasio Fuerza', 'PRO', 99.90, 'CANCELADA', CURRENT_DATE - 140, CURRENT_DATE - 80)
    RETURNING id, cliente
),
cobros (cliente, monto, meses_atras, dia, metodo) AS (
    VALUES
    -- Hace 5 meses
    ('Panadería Doña Rosa', 49.90, 5, 4, 'YAPE'),
    ('Librería Cerrada', 49.90, 5, 9, 'TARJETA'),
    ('Gimnasio Fuerza', 99.90, 5, 12, 'TARJETA'),
    -- Hace 4 meses
    ('Panadería Doña Rosa', 49.90, 4, 4, 'YAPE'),
    ('Café Central', 99.90, 4, 6, 'TARJETA'),
    ('Librería Cerrada', 49.90, 4, 9, 'TARJETA'),
    ('Gimnasio Fuerza', 99.90, 4, 12, 'TARJETA'),
    -- Hace 3 meses: sin pagos, para ver un mes en cero
    -- Hace 2 meses
    ('Panadería Doña Rosa', 49.90, 2, 4, 'YAPE'),
    ('Café Central', 99.90, 2, 6, 'TARJETA'),
    ('Tienda Don Pedro', 99.90, 2, 8, 'YAPE'),
    ('Barbería El Corte', 49.90, 2, 10, 'YAPE'),
    -- El mes pasado
    ('Panadería Doña Rosa', 49.90, 1, 4, 'YAPE'),
    ('Café Central', 99.90, 1, 6, 'TARJETA'),
    ('Tienda Don Pedro', 99.90, 1, 8, 'YAPE'),
    ('Barbería El Corte', 49.90, 1, 10, 'YAPE'),
    ('Bodega San Martín', 49.90, 1, 15, 'YAPE'),
    -- Este mes
    ('Panadería Doña Rosa', 49.90, 0, 1, 'YAPE'),
    ('Café Central', 99.90, 0, 1, 'TARJETA'),
    ('Tienda Don Pedro', 99.90, 0, 1, 'YAPE')
)
INSERT INTO pago (suscripcion_id, monto, fecha_pago, metodo)
SELECT
    nuevas.id,
    cobros.monto,
    (date_trunc('month', CURRENT_DATE) - make_interval(months => cobros.meses_atras) + make_interval(days => cobros.dia - 1))::date,
    cobros.metodo
FROM cobros
JOIN nuevas ON nuevas.cliente = cobros.cliente;
