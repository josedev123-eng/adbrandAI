// Formato de los números de la pantalla de planes.
export const NUMERO = new Intl.NumberFormat('es-PE')

// La descripción viene de la base con "50 000"; se escribe igual que el resto de la página ("50,000").
export function conMiles(texto) {
  return String(texto ?? '').replace(/\b(\d{1,3}) (\d{3})\b/g, '$1,$2')
}

// Un anuncio con sus 4 opciones gasta unos 5 000 tokens de IA (promedio medido con anuncios reales).
// Sirve para decir el plan en algo que el dueño entiende: "≈ 30 anuncios al mes".
export const TOKENS_POR_ANUNCIO = 5000

export function anunciosAprox(tokens) {
  return Math.max(1, Math.floor(Number(tokens) / TOKENS_POR_ANUNCIO))
}

// Lo que trae cualquier plan (todo está disponible desde el plan gratuito; los planes dan más IA al mes).
export const VENTAJAS_COMUNES = [
  'Imagen final con tu logo, colores y eslogan',
  'Video para Reels y Estados de WhatsApp',
  'Afiche con QR para pedir por WhatsApp',
  'Calendario con fechas que venden en Perú',
  'Kit de marca y manual en PDF',
  'Revisión de tu oferta con las reglas de INDECOPI',
]

// Las ventajas de un plan, de la más importante a la menos: primero cuánto alcanza.
export function ventajasDe(plan) {
  const ventajas = [
    `≈ ${NUMERO.format(anunciosAprox(plan.limiteTokens))} anuncios con 4 opciones al mes`,
    `${NUMERO.format(Number(plan.limiteTokens))} tokens de IA al mes`,
    ...VENTAJAS_COMUNES,
  ]
  if (/premium/i.test(plan.nombre ?? '')) ventajas.push('Atención preferente por Contacto')
  return ventajas
}
