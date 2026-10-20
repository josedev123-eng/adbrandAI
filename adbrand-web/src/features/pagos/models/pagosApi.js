// Model: llamadas a la API de pagos y planes (HU 25 a 28).
import { apiRequest } from '../../../shared/services/apiClient'

export function obtenerPlanes() {
  return apiRequest('/planes')
}
