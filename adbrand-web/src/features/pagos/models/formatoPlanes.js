// Formato de los números de la pantalla de planes.
export const NUMERO = new Intl.NumberFormat('es-PE')

// La descripción viene de la base con "50 000"; se escribe igual que el resto de la página ("50,000").
export function conMiles(texto) {
  return String(texto ?? '').replace(/\b(\d{1,3}) (\d{3})\b/g, '$1,$2')
}
