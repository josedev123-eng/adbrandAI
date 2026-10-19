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

// HU-24: siempre responde 200 con el mismo mensaje, exista o no el correo.
export function solicitarCodigo(correo) {
  return apiRequest('/auth/recuperar/solicitar', { method: 'POST', body: { correo } })
}

// HU-24: 400 CODIGO_INVALIDO si el código está mal, venció o ya se usó.
export function cambiarContrasena(datos) {
  return apiRequest('/auth/recuperar/cambiar', { method: 'POST', body: datos })
}
