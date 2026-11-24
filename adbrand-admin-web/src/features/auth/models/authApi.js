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

// Mi cuenta: cambia la contraseña del administrador conectado (debe escribir la actual).
export function cambiarMiContrasena(actual, nueva) {
  return apiRequest('/usuarios/mi-contrasena/', { method: 'POST', body: { actual, nueva } })
}

// Olvidé mi contraseña, paso 1: Django manda un código de 6 números al correo.
export function solicitarCodigo(correo) {
  return apiRequest('/usuarios/recuperar/solicitar/', { method: 'POST', body: { correo } })
}

// Olvidé mi contraseña, paso 2: con el código se pone una contraseña nueva.
export function cambiarConCodigo(correo, codigo, nueva) {
  return apiRequest('/usuarios/recuperar/cambiar/', { method: 'POST', body: { correo, codigo, nueva } })
}
