// HU 63: la idea elegida en "Tu mes de contenido" se pasa a "Crear anuncio" para que llegue ya escrita.
// Se guarda solo para esta pestaña del navegador; "Crear anuncio" la lee al abrir y luego la borra. Si el navegador no deja guardar, no pasa nada.
const CLAVE = 'adbrand.ofertaSugerida'

export function guardarOfertaSugerida(oferta) {
  try {
    sessionStorage.setItem(CLAVE, oferta)
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

export function olvidarOfertaSugerida() {
  try {
    sessionStorage.removeItem(CLAVE)
  } catch {
    // nada que borrar
  }
}
