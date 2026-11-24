// Rendimiento: guarda por un rato la respuesta de consultas que casi no cambian (planes, kit de marca, perfil)
// para no pedirlas otra vez cada vez que el usuario cambia de pantalla.
// - Si dos pantallas piden lo mismo a la vez, sale un solo pedido al servidor.
// - Cada sesión tiene su propia caché (la clave incluye el token): otro usuario nunca ve datos ajenos.
// - Si el pedido falla no se guarda nada, así el siguiente intento vuelve a preguntar al servidor.
// - Al guardar cambios, el modelo llama a olvidarCache para que la siguiente lectura traiga lo nuevo.
import { leerToken } from './sesionToken'

const memoria = new Map()

export const UN_MINUTO = 60_000

function claveCompleta(clave) {
  return `${leerToken() ?? 'sin-sesion'}|${clave}`
}

export function conCache(clave, pedir, { duracion = 5 * UN_MINUTO } = {}) {
  const completa = claveCompleta(clave)
  const guardado = memoria.get(completa)
  if (guardado && guardado.vence > Date.now()) return guardado.promesa

  const promesa = pedir()
  memoria.set(completa, { promesa, vence: Date.now() + duracion })
  promesa.catch(() => {
    if (memoria.get(completa)?.promesa === promesa) memoria.delete(completa)
  })
  return promesa
}

// Borra lo guardado de una consulta (en todas las sesiones de este navegador).
export function olvidarCache(clave) {
  for (const completa of memoria.keys()) {
    if (completa.endsWith(`|${clave}`)) memoria.delete(completa)
  }
}

export function limpiarCache() {
  memoria.clear()
}

// Para usar tras guardar: apiRequest(...).then(olvidarDespues('perfil')) devuelve la respuesta y olvida la caché.
export function olvidarDespues(clave) {
  return (respuesta) => {
    olvidarCache(clave)
    return respuesta
  }
}
