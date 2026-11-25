// Model: llamadas a la API de los datos de la empresa (HU-23).
// No llevan id: Spring usa siempre la empresa del usuario que inició sesión.
import { apiRequest } from '../../../shared/services/apiClient'

export function obtenerEmpresa() {
  return apiRequest('/cuenta/empresa')
}

// 400 con "campos" si algo está mal escrito; 409 RUC_REPETIDO si otra cuenta ya tiene ese RUC.
export function guardarEmpresa(datos) {
  return apiRequest('/cuenta/empresa', { method: 'PUT', body: datos })
}

// Cambiar la contraseña escribiendo la actual. 400 CONTRASENA_ACTUAL_INCORRECTA o CONTRASENA_IGUAL.
export function cambiarMiContrasena(actual, nueva) {
  return apiRequest('/cuenta/contrasena', { method: 'POST', body: { actual, nueva } })
}

// Avatar con imagen: una foto (data URL) o el logo del kit. Devuelve el usuario con "avatarImagen" ya reducida.
export function cambiarAvatar({ imagen, usarLogoDelKit = false }) {
  return apiRequest('/cuenta/avatar', { method: 'PUT', body: { imagen, usarLogoDelKit } })
}

export function quitarAvatar() {
  return apiRequest('/cuenta/avatar', { method: 'DELETE' })
}
