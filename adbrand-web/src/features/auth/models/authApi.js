// Model: llamadas a la API de cuentas del usuario PYME (HU-21, HU-22).
import { apiRequest } from '../../../shared/services/apiClient'

// Responde 201 con { id, nombre, nombreEmpresa, correo }; 409 CORREO_REPETIDO si el correo ya existe.
// Spring manda al correo un código de 6 dígitos: la cuenta no entra hasta confirmarlo (V56).
export function registrar(datos) {
  return apiRequest('/auth/registro', { method: 'POST', body: datos })
}

// HU-22: responde { token, usuario }; 401 CREDENCIALES_INVALIDAS, 403 CUENTA_INACTIVA
// 403 CORREO_NO_VERIFICADO (falta el código del correo) y 429 CUENTA_BLOQUEADA (5 contraseñas equivocadas seguidas: 15 minutos sin entrar).
export function iniciarSesion(correo, contrasena) {
  return apiRequest('/auth/login', { method: 'POST', body: { correo, contrasena } })
}

// Confirmar el correo con el código: responde { token, usuario } (entra directo); 400 CODIGO_INVALIDO si no.
export function verificarCorreo(correo, codigo) {
  return apiRequest('/auth/verificar-correo', { method: 'POST', body: { correo, codigo } })
}

// Pide otro código de verificación. Siempre 200 con el mismo mensaje, exista o no la cuenta.
export function reenviarCodigoVerificacion(correo) {
  return apiRequest('/auth/verificar-correo/reenviar', { method: 'POST', body: { correo } })
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
