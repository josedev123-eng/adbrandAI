// Model: llamadas a la API de moderación (HU-14).
import { apiRequest } from '../../../shared/services/apiClient'

export const NOMBRES_RED = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook' }
export const NOMBRES_TONO = { CERCANO: 'Cercano', PROFESIONAL: 'Profesional', DIVERTIDO: 'Divertido', ELEGANTE: 'Elegante' }
export const NOMBRES_ESTADO = { APROBADO: 'Aprobado', DUDOSO: 'Dudoso', RECHAZADO: 'Rechazado' }

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