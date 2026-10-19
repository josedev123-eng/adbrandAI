// Guarda el token del usuario PYME en el navegador (HU-22). Si el navegador no deja usar
// localStorage (modo privado, permisos), la sesión dura solo mientras la pestaña esté abierta.
const CLAVE = 'adbrand-token'

export function guardarToken(token) {
  try {
    localStorage.setItem(CLAVE, token)
  } catch {
    // Sin localStorage no se puede recordar la sesión; no es un error que deba ver el usuario.
  }
}

export function leerToken() {
  try {
    return localStorage.getItem(CLAVE)
  } catch {
    return null
  }
}

export function borrarToken() {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // Nada que borrar.
  }
}
