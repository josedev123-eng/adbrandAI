// Model: las PYMEs registradas y su suspensión (Django: /api/empresas/).
import { apiRequest } from '../../../shared/services/apiClient'

export const MOTIVO_MINIMO = 10
export const MOTIVO_MAXIMO = 300

export function listarEmpresas({ busqueda, estado } = {}) {
  const parametros = new URLSearchParams()
  if (busqueda) parametros.set('busqueda', busqueda)
  if (estado) parametros.set('estado', estado)
  const consulta = parametros.toString()
  return apiRequest(`/empresas/${consulta ? `?${consulta}` : ''}`)
}

export function suspenderEmpresa(id, motivo) {
  return apiRequest(`/empresas/${id}/suspender/`, { method: 'POST', body: { motivo } })
}

export function reactivarEmpresa(id) {
  return apiRequest(`/empresas/${id}/reactivar/`, { method: 'POST' })
}
