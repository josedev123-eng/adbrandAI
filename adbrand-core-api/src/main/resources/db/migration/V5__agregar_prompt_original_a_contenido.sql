-- HU 12: agregar columna para guardar el prompt original usado para generar el contenido.
ALTER TABLE contenido ADD COLUMN prompt_original TEXT;

COMMENT ON COLUMN contenido.prompt_original IS 'Prompt completo enviado a la IA para generar este contenido (HU 12)';