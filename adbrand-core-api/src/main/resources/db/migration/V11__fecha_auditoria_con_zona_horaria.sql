-- HU 04: Django guarda la fecha en UTC (USE_TZ = True). Con zona horaria la hora se muestra bien en Lima.
ALTER TABLE auditoria_admin ALTER COLUMN fecha TYPE TIMESTAMPTZ USING fecha AT TIME ZONE 'UTC';
