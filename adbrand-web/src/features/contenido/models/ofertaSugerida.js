// HU 63: la idea elegida en "Tu mes de contenido" se pasa a "Crear anuncio" para que llegue ya escrita.
// Se guarda solo para esta pestaña del navegador; "Crear anuncio" la lee al abrir y luego la borra. Si el navegador no deja guardar, no pasa nada.
// "ocasion" es la festividad de la fecha (Halloween, Navidad…): el anuncio se ambienta en ella.
const CLAVE = 'adbrand.ofertaSugerida'
const CLAVE_OCASION = 'adbrand.ocasionSugerida'

export function guardarOfertaSugerida(oferta, ocasion) {
  try {
    sessionStorage.setItem(CLAVE, oferta)
    if (ocasion) sessionStorage.setItem(CLAVE_OCASION, ocasion)
    else sessionStorage.removeItem(CLAVE_OCASION)
  } catch {
    // sin almacenamiento: "Crear anuncio" abre vacío, como siempre
  }
}

export function leerOfertaSugerida() {
  try {
    return sessionStorage.getItem(CLAVE) ?? ''
  } catch {
    return ''
  }
}

export function leerOcasionSugerida() {
  try {
    return sessionStorage.getItem(CLAVE_OCASION) ?? ''
  } catch {
    return ''
  }
}

export function olvidarOfertaSugerida() {
  try {
    sessionStorage.removeItem(CLAVE)
    sessionStorage.removeItem(CLAVE_OCASION)
  } catch {
    // nada que borrar
  }
}
