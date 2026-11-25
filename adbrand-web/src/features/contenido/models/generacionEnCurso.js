// Model: la creación de las 4 opciones que está en curso. Vive fuera de la pantalla a propósito:
// si el dueño sale de "Crear anuncio" mientras la IA trabaja (por ejemplo, a Planes), el pedido sigue
// y, al volver, ve sus opciones (antes se perdía y parecía que no había pasado nada).
let actual = null // { solicitud, promesa, estado: 'creando' | 'lista' | 'error', respuesta, error, inicio }
const oyentes = new Set()

function avisar() {
  oyentes.forEach((oyente) => oyente(actual))
}

export function iniciarGeneracion(solicitud, pedir) {
  const promesa = pedir(solicitud)
  actual = { solicitud, promesa, estado: 'creando', inicio: Date.now() }
  avisar()
  promesa.then(
    (respuesta) => {
      if (actual?.promesa !== promesa) return
      actual = { ...actual, estado: 'lista', respuesta }
      avisar()
    },
    (error) => {
      if (actual?.promesa !== promesa) return
      actual = { ...actual, estado: 'error', error }
      avisar()
    },
  )
  return promesa
}

export function generacionActual() {
  return actual
}

// La pantalla ya mostró el resultado (o el error): se olvida.
export function olvidarGeneracion() {
  actual = null
  avisar()
}

export function suscribirGeneracion(oyente) {
  oyentes.add(oyente)
  return () => oyentes.delete(oyente)
}
