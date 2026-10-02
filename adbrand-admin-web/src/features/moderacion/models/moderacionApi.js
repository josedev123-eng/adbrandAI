// Model: llamadas a la API de moderación (HU-14).
import { apiRequest } from '../../../shared/services/apiClient'

export const NOMBRES_RED = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook' }

export function listarDudosos() {
  return apiRequest('/moderacion/contenidos/dudosos/')
}

export function obtenerContenido(id) {
  return apiRequest(`/moderacion/contenidos/${id}/`)
}

const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

export function formatearFecha(texto) {
  return FORMATO_FECHA.format(new Date(texto))
}