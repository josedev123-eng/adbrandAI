// Model: llamadas a la API del calendario de publicaciones (HU 33).
import { apiRequest } from '../../../shared/services/apiClient'

// HU 35: con campanaId solo llegan las publicaciones de esa campaña.
export function obtenerCalendario(mes, campanaId) {
  const filtro = campanaId ? `&campanaId=${encodeURIComponent(campanaId)}` : ''
  return apiRequest(`/calendario?mes=${encodeURIComponent(mes)}${filtro}`)
}

export function crearPublicacion(datos) {
  return apiRequest('/calendario', { method: 'POST', body: datos })
}

// HU 36: mueve la publicación a otro día; la hora no cambia.
export function reprogramarPublicacion(id, fecha) {
  return apiRequest(`/calendario/${id}/fecha`, { method: 'PATCH', body: { fecha } })
}
