// Model: llamadas a la API del estado del servidor (HU-18).
import { apiRequest } from '../../../shared/services/apiClient'

// Verifica ahora y devuelve { verificado_en, servicios: [{ servicio, nombre, estado, tiempo_ms, detalle }] }.
// estado es ACTIVO o CAIDO.
export function verificarEstado() {
  return apiRequest('/servidor/estado/')
}

const FORMATO_HORA = new Intl.DateTimeFormat('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })

// "2026-10-27T09:05:03-05:00" → "09:05:03"
export function formatearHora(texto) {
  return FORMATO_HORA.format(new Date(texto))
}
