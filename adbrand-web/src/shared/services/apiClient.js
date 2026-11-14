// Cliente HTTP único hacia el backend del usuario (Spring Boot).
// Todos los errores salen con un mensaje claro en español (error.message) y un código (error.codigo):
// - servidor apagado o sin internet → SIN_CONEXION
// - el servidor tarda demasiado → TIEMPO_AGOTADO
// - errores HTTP → el mensaje que manda Spring, o uno según el estado (403, 404, 429, 500...)
import { borrarToken, leerToken } from './sesionToken'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

// HU-22: lo escucha SesionProvider para volver al inicio de sesión.
export const EVENTO_SESION_VENCIDA = 'adbrand:sesion-vencida'

// Tiempo máximo de espera. Las pantallas con IA pasan uno más largo (TIEMPO_IA).
export const TIEMPO_NORMAL = 30_000
export const TIEMPO_IA = 180_000

export const MENSAJES = {
  SIN_INTERNET: 'No tienes conexión a internet. Revisa tu wifi o tus datos e inténtalo de nuevo.',
  SIN_CONEXION: 'No pudimos conectar con el servidor. Revisa tu conexión o inténtalo en unos minutos.',
  TIEMPO_AGOTADO: 'El servidor está tardando demasiado en responder. Inténtalo de nuevo en unos minutos.',
  SESION_VENCIDA: 'Tu sesión venció. Vuelve a iniciar sesión.',
  SIN_PERMISO: 'No tienes permiso para hacer esto.',
  NO_ENCONTRADO: 'No encontramos lo que buscas. Puede que se haya borrado.',
  DEMASIADOS_INTENTOS: 'Hiciste demasiados intentos seguidos. Espera un momento y vuelve a intentarlo.',
  ERROR_SERVIDOR: 'El servidor tuvo un problema. Ya lo estamos revisando; inténtalo en unos minutos.',
  SERVICIO_NO_DISPONIBLE: 'El servicio no está disponible en este momento. Inténtalo en unos minutos.',
  GENERICO: 'La solicitud no se pudo completar. Inténtalo de nuevo.',
}

// Mensaje según el estado HTTP, para cuando el servidor no manda uno propio.
export function mensajePorEstado(status) {
  if (status === 401) return MENSAJES.SESION_VENCIDA
  if (status === 403) return MENSAJES.SIN_PERMISO
  if (status === 404) return MENSAJES.NO_ENCONTRADO
  if (status === 429) return MENSAJES.DEMASIADOS_INTENTOS
  if (status === 502 || status === 503 || status === 504) return MENSAJES.SERVICIO_NO_DISPONIBLE
  if (status >= 500) return MENSAJES.ERROR_SERVIDOR
  return MENSAJES.GENERICO
}

function crearError(mensaje, { status = 0, codigo, campos = {} } = {}) {
  const error = new Error(mensaje)
  error.status = status
  error.codigo = codigo
  error.campos = campos
  return error
}

// fetch con tiempo máximo. Si falla la red o se agota el tiempo, lanza un error con mensaje claro.
async function pedir(url, opciones, tiempoMaximo) {
  const controlador = new AbortController()
  const temporizador = setTimeout(() => controlador.abort(), tiempoMaximo)
  try {
    return await fetch(url, { ...opciones, signal: controlador.signal })
  } catch (falla) {
    if (falla?.name === 'AbortError') {
      throw crearError(MENSAJES.TIEMPO_AGOTADO, { codigo: 'TIEMPO_AGOTADO' })
    }
    const sinInternet = typeof navigator !== 'undefined' && navigator.onLine === false
    throw crearError(sinInternet ? MENSAJES.SIN_INTERNET : MENSAJES.SIN_CONEXION, { codigo: 'SIN_CONEXION' })
  } finally {
    clearTimeout(temporizador)
  }
}

export async function apiRequest(ruta, { method = 'GET', body, tiempoMaximo = TIEMPO_NORMAL } = {}) {
  // HU-22: si hay sesión, cada petición lleva el token.
  const token = leerToken()
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const respuesta = await pedir(
    `${BASE_URL}${ruta}`,
    { method, headers, body: body ? JSON.stringify(body) : undefined },
    tiempoMaximo,
  )

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) lanzarError(respuesta, datos, token)
  return datos
}

// HU-38: para descargar archivos (PDF, Word). Devuelve un Blob en lugar de JSON.
export async function apiDescargar(ruta, { tiempoMaximo = TIEMPO_IA } = {}) {
  const token = leerToken()
  const respuesta = await pedir(
    `${BASE_URL}${ruta}`,
    { headers: token ? { Authorization: `Bearer ${token}` } : {} },
    tiempoMaximo,
  )

  if (!respuesta.ok) lanzarError(respuesta, await respuesta.json().catch(() => null), token)
  return respuesta.blob()
}

function lanzarError(respuesta, datos, token) {
  // HU-22: un 401 con token significa que la sesión venció o se alteró. El 401 del propio
  // inicio de sesión (sin token) no cierra nada: solo es una contraseña equivocada.
  if (respuesta.status === 401 && token) {
    borrarToken()
    window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA))
  }

  // Spring devuelve { codigo, mensaje, campos } (ver shared/error/ErrorResponse.java).
  // Si no manda mensaje (por ejemplo, un 502 de un proxy), se usa uno según el estado.
  throw crearError(datos?.mensaje || mensajePorEstado(respuesta.status), {
    status: respuesta.status,
    codigo: datos?.codigo,
    campos: datos?.campos ?? {},
  })
}
