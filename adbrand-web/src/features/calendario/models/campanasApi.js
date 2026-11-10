// Model: llamadas a la API de campañas y eventos especiales (HU 35).
import { apiRequest } from '../../../shared/services/apiClient'

export function listarCampanas() {
  return apiRequest('/campanas')
}

export function crearCampana(datos) {
  return apiRequest('/campanas', { method: 'POST', body: datos })
}
