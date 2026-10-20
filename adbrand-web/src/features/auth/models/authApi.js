// Model: llamadas a la API de cuentas del usuario PYME (HU-21, HU-22).
import { apiRequest } from '../../../shared/services/apiClient'

// Responde 201 con { id, nombre, nombreEmpresa, correo }; 409 CORREO_REPETIDO si el correo ya existe.
export function registrar(datos) {
  return apiRequest('/auth/registro', { method: 'POST', body: datos })
}

// HU-22: responde { token, usuario }; 401 CREDENCIALES_INVALIDAS y 403 CUENTA_INACTIVA.
export function iniciarSesion(correo, contrasena) {
  return apiRequest('/auth/login', { method: 'POST', body: { correo, contrasena } })
}

// HU-22: el usuario del token guardado. Se pide al abrir o recargar la página.
export function obtenerSesion() {
  return apiRequest('/auth/sesion')
}
