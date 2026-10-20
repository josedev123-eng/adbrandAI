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

// Quién hace el cambio ya no se manda a mano: Django lo saca del token de sesión.
export function desactivarAdministrador(id) {
  return apiRequest(`/usuarios/administradores/${id}/desactivar/`, {
    method: 'PATCH',
    body: { confirmar: true },
  })
}

// HU-02: los permisos de un administrador dependen de su rol.
export function cambiarRolAdministrador(id, rolId) {
  return apiRequest(`/usuarios/administradores/${id}/rol/`, {
    method: 'PATCH',
    body: { rol_id: rolId },
  })
}
