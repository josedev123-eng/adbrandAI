// Model: llamadas a la API de suscripciones (HU-08).
import { apiRequest } from '../../../shared/services/apiClient'

export const ESTADOS_SUSCRIPCION = {
  ACTIVA: 'Activa',
  VENCIDA: 'Vencida',
  PENDIENTE_PAGO: 'Pendiente de pago',
  CANCELADA: 'Cancelada',
}

// Estados que el endpoint /con-deuda/ sabe filtrar.
export const ESTADOS_CON_DEUDA = ['VENCIDA', 'PENDIENTE_PAGO']

export function listarSuscripciones() {
  return apiRequest('/suscripciones/con-deuda/')
}

export function listarSuscripcionesPorEstado(estado) {
  if (!estado) return listarSuscripciones()
  return apiRequest(`/suscripciones/con-deuda/?estado=${estado}`)
}