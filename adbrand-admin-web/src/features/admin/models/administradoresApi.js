// Model: llamadas a la API del módulo de administradores (HU-01).
import { apiRequest } from '../../../shared/services/apiClient'

export function listarRoles() {
  return apiRequest('/usuarios/roles/')
}

export function crearAdministrador(datos) {
  return apiRequest('/usuarios/administradores/', { method: 'POST', body: datos })
}