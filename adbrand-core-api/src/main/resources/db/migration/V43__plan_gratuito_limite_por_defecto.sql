-- El "límite por defecto" de limite_tokens (usuario_id NULL) pasa a ser el plan gratuito: lo que puede usar
-- una empresa que no tiene un plan pagado activo. Estaba en 100 000, más que el plan Básico (50 000).
-- Solo se cambia si nadie lo modificó desde el panel del administrador.
UPDATE limite_tokens
SET limite_mensual = 10000, actualizado = now()
WHERE usuario_id IS NULL AND limite_mensual = 100000;
