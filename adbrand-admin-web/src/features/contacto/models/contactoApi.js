// Model: los mensajes del formulario de Contacto de la web (Django: /api/contacto/).
import { apiRequest } from '../../../shared/services/apiClient'

export function listarMensajes({ pendientes = false } = {}) {
  return apiRequest(`/contacto/${pendientes ? '?pendientes=1' : ''}`)
}

export function marcarAtendido(id, atendido) {
  return apiRequest(`/contacto/${id}/`, { method: 'PATCH', body: { atendido } })
}
