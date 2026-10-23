// Model: llamadas a la API de pagos y planes (HU 25 a 28).
import { apiRequest } from '../../../shared/services/apiClient'

export function obtenerPlanes() {
  return apiRequest('/planes')
}

export function pagarConTarjeta(datos) {
  return apiRequest('/pagos/tarjeta', { method: 'POST', body: datos })
}

// HU 27: genera el QR de Yape (operación pendiente) y luego la confirma.
export function registrarYape(datos) {
  return apiRequest('/pagos/yape', { method: 'POST', body: datos })
}

export function confirmarYape(operacionId) {
  return apiRequest(`/pagos/yape/${operacionId}/confirmar`, { method: 'POST' })
}

export function obtenerYapePendientes() {
  return apiRequest('/pagos/yape/pendientes')
}
