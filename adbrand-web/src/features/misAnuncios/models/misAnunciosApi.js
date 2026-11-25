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

// "Cancelar publicación": el anuncio ya no se publica y sus fechas del calendario se cancelan.
// Responde { programacionesCanceladas: n }.
export function cancelarPublicacion(id) {
  return apiRequest(`/contenido/${id}/cancelar-publicacion`, { method: 'POST' })
}

// "Volver a activar" un anuncio cancelado.
export function reactivarAnuncio(id) {
  return apiRequest(`/contenido/${id}/reactivar`, { method: 'POST' })
}

// HU 63: eliminar de la lista un anuncio cancelado (400 SOLO_CANCELADOS si no lo está).
export function eliminarAnuncio(id) {
  return apiRequest(`/contenido/${id}`, { method: 'DELETE' })
}

export function marcarAvisosVistos() {
  return apiRequest('/contenido/avisos/vistos', { method: 'POST' })
}
