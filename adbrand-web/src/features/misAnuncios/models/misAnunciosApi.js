// Model: "Mis anuncios" y los avisos de lo que decidió el moderador.
import { apiRequest } from '../../../shared/services/apiClient'

export const ESTADOS = {
  APROBADO: { nombre: 'Aprobado', clase: 'estado-aprobado' },
  DUDOSO: { nombre: 'En revisión', clase: 'estado-revision' },
  RECHAZADO: { nombre: 'Rechazado', clase: 'estado-rechazado' },
}

export function listarMisAnuncios() {
  return apiRequest('/contenido/mis-anuncios')
}

// { pendientes: n }: decisiones del moderador que el dueño todavía no vio (el número del menú).
export function contarAvisos() {
  return apiRequest('/contenido/avisos')
}

export function marcarAvisosVistos() {
  return apiRequest('/contenido/avisos/vistos', { method: 'POST' })
}
