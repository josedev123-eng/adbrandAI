-- Datos de prueba para editar el kit de marca (HU 32).
-- Ejecutar MANUALMENTE en la base de datos, una sola vez. No es una migración Flyway.
-- Encoding: UTF-8
--
-- Con AI_MOCK=true generar un kit (HU 11) todavía falla, porque el modo simulado devuelve
-- un anuncio y no el JSON del kit. Este archivo deja un kit listo para editar.
-- Cambia el correo por el de la cuenta con la que inicias sesión en la web.

INSERT INTO kit_marca (usuario_id, nombre_negocio, sector, paleta_sugerida, valores_eslogan, estilo_visual,
                       logo_concepto, tipografia_primaria, tipografia_secundaria, paleta_colores, voz_marca)
SELECT id,
       'Bodega San Martín',
       'Bodega y minimarket',
       'Verdes y amarillos',
       'Cerca de ti, todos los días',
       'MODERNO',
       'Una canasta sencilla con una hoja verde encima, trazo grueso y esquinas redondeadas.',
       'Bricolage Grotesque',
       'Plus Jakarta Sans',
       E'#2E7D32 - Verde bodega - Confianza\n#F9A825 - Amarillo sol - Ofertas\n#FFF8E1 - Crema - Fondo',
       'Cercana y directa, como el bodeguero que te conoce por tu nombre. Usa tú.'
FROM usuario
WHERE correo = 'luis@bodega.pe';
