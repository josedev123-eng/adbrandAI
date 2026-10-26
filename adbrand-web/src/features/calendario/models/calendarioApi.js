// Model: llamadas a la API del calendario de publicaciones (HU 33).
import { apiRequest } from '../../../shared/services/apiClient'

export function obtenerCalendario(mes) {
  return apiRequest(`/calendario?mes=${encodeURIComponent(mes)}`)
}

export function crearPublicacion(datos) {
  return apiRequest('/calendario', { method: 'POST', body: datos })
}
