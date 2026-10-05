// Model: llamadas a la API del módulo de administradores (HU-01, HU-03).
import { apiRequest } from '../../../shared/services/apiClient'

export function listarRoles() {
  return apiRequest('/usuarios/roles/')
}

export function crearAdministrador(datos) {
  return apiRequest('/usuarios/administradores/', { method: 'POST', body: datos })
}

export function listarAdministradores() {
  return apiRequest('/usuarios/administradores/')
}

export function desactivarAdministrador(id) {
  return apiRequest(`/usuarios/administradores/${id}/desactivar/`, {
    method: 'PATCH',
    body: { confirmar: true },
  })
}