-- HU 20: consumo de tokens de IA agrupado por mes y por empresa, para revisar el historial por periodo.
-- El mes se calcula con la hora de Lima, igual que el panel de la HU 17.
CREATE VIEW vista_consumo_token_mensual AS
SELECT
    date_trunc('month', fecha AT TIME ZONE 'America/Lima')::date AS mes,
    usuario_id,
    SUM(tokens_total) AS tokens,
    COUNT(*) AS generaciones
FROM consumo_token
GROUP BY 1, usuario_id;
