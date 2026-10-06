// Model: llamadas a la API del módulo de administradores (HU-01, HU-03).
import { apiRequest } from '../../../shared/services/apiClient'

// Mientras no existe el inicio de sesión (HU 02), la web manda siempre el mismo
// administrador. Cuando haya login, este valor se reemplaza por el de la sesión.
export const ADMIN_DE_PRUEBA = 1

export function listarRoles() {
  return apiRequest('/usuarios/roles/')
}

export function crearAdministrador(datos) {
  return apiRequest('/usuarios/administradores/', { method: 'POST', body: datos })
}

export function listarAdministradores() {
  return apiRequest('/usuarios/administradores/')
}

export function desactivarAdministrador(id, adminId = ADMIN_DE_PRUEBA) {
  return apiRequest(`/usuarios/administradores/${id}/desactivar/`, {
    method: 'PATCH',
    body: { confirmar: true },
    headers: { 'X-Admin-Id': String(adminId) },
  })
}