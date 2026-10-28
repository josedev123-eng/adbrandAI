// Model: llamadas a la API de informes (HU-37).
// No llevan id: Spring usa siempre la empresa del usuario que inició sesión.
import { apiRequest } from '../../../shared/services/apiClient'

// Devuelve { empresa, total, aprobados, dudosos, rechazados, campanas: [{ id, fecha, redSocial, oferta, estado }] }.
export function obtenerInformeCampanas() {
  return apiRequest('/reportes/campanas')
}

export const NOMBRES_RED = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook' }
export const NOMBRES_ESTADO = { APROBADO: 'Aprobado', DUDOSO: 'En revisión', RECHAZADO: 'Rechazado' }

// "2026-11-03T10:05:00" → "03/11/2026 10:05"
export function formatearFecha(texto) {
  const fecha = new Date(texto)
  const dos = (numero) => String(numero).padStart(2, '0')
  return `${dos(fecha.getDate())}/${dos(fecha.getMonth() + 1)}/${fecha.getFullYear()} ${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
}
