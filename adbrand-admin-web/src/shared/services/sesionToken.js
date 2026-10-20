// Guarda el token de sesión del administrador en el navegador (login de administradores).
// localStorage puede fallar (modo privado o bloqueado), por eso cada uso va en try/catch.
const CLAVE = 'adbrand-admin-token'

export function leerToken() {
  try {
    return localStorage.getItem(CLAVE)
  } catch {
    return null
  }
}

export function guardarToken(token) {
  try {
    localStorage.setItem(CLAVE, token)
  } catch {
    // Sin localStorage la sesión dura solo mientras la pestaña esté abierta.
  }
}

export function borrarToken() {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // Nada que borrar.
  }
}
