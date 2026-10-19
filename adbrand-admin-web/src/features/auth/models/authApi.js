// Model: inicio de sesión de administradores (Sprint 2, tarea técnica de login).
import { apiRequest } from '../../../shared/services/apiClient'

// Devuelve { token, admin } si el correo y la contraseña son correctos.
export function iniciarSesion(correo, contrasena) {
  return apiRequest('/usuarios/login/', { method: 'POST', body: { correo, contrasena } })
}

// Con el token guardado, pregunta a Django quién está conectado.
export function obtenerSesion() {
  return apiRequest('/usuarios/sesion/')
}
