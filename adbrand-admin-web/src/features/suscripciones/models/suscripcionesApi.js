// Model: llamadas a la API de suscripciones (HU-08).
import { apiRequest } from '../../../shared/services/apiClient'

export const ESTADOS_SUSCRIPCION = {
  ACTIVA: 'Activa',
  VENCIDA: 'Vencida',
  PENDIENTE_PAGO: 'Pendiente de pago',
  CANCELADA: 'Cancelada',
}

export function listarSuscripciones() {
  return apiRequest('/suscripciones/')
}

export function obtenerSuscripcion(id) {
  return apiRequest(`/suscripciones/${id}/`)
}