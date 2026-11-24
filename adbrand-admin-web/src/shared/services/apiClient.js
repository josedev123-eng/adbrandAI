// Cliente HTTP único hacia el backend de administración (Django).
// Todos los errores salen con un mensaje claro en español (error.message) y un código (error.codigo):
// - Django apagado o sin internet → SIN_CONEXION
// - el servidor tarda demasiado → TIEMPO_AGOTADO
// - errores HTTP → el "detail" que manda Django, o uno según el estado (403, 404, 429, 500...)
// error.detalles sigue trayendo lo que manda Django (por ejemplo { campo: ["mensaje"] }).
import { borrarToken, leerToken } from './sesionToken'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8000/api'

// Aviso para toda la web: el token venció o es falso, hay que volver al login.
export const EVENTO_SESION_VENCIDA = 'adbrand:sesion-vencida'

export const TIEMPO_NORMAL = 30_000

export const MENSAJES = {
  SIN_INTERNET: 'No tienes conexión a internet. Revisa tu wifi o tus datos e inténtalo de nuevo.',
  SIN_CONEXION: 'No se pudo conectar con el servidor de administración. Revisa que Django esté encendido.',
  TIEMPO_AGOTADO: 'El servidor de administración está tardando demasiado en responder. Inténtalo de nuevo.',
  SESION_VENCIDA: 'Tu sesión venció. Vuelve a iniciar sesión.',
  SIN_PERMISO: 'Tu rol no tiene permiso para hacer esto.',
  NO_ENCONTRADO: 'No encontramos lo que buscas. Puede que se haya borrado.',
  DEMASIADOS_INTENTOS: 'Hiciste demasiados intentos seguidos. Espera un momento y vuelve a intentarlo.',
  ERROR_SERVIDOR: 'El servidor de administración tuvo un error. Revisa la consola de Django.',
  SERVICIO_NO_DISPONIBLE: 'El servicio no está disponible en este momento. Inténtalo en unos minutos.',
  GENERICO: 'La solicitud no se pudo completar',
}

// Mensaje según el estado HTTP, para cuando Django no manda un "detail".
export function mensajePorEstado(status) {
  if (status === 401) return MENSAJES.SESION_VENCIDA
  if (status === 403) return MENSAJES.SIN_PERMISO
  if (status === 404) return MENSAJES.NO_ENCONTRADO
  if (status === 429) return MENSAJES.DEMASIADOS_INTENTOS
  if (status === 502 || status === 503 || status === 504) return MENSAJES.SERVICIO_NO_DISPONIBLE
  if (status >= 500) return MENSAJES.ERROR_SERVIDOR
  return MENSAJES.GENERICO
}

function crearError(mensaje, { status = 0, codigo, detalles = null } = {}) {
  const error = new Error(mensaje)
  error.status = status
  error.codigo = codigo
  error.detalles = detalles
  return error
}

export async function apiRequest(ruta, { method = 'GET', body, headers = {}, tiempoMaximo = TIEMPO_NORMAL } = {}) {
  const token = leerToken()
  const controlador = new AbortController()
  const temporizador = setTimeout(() => controlador.abort(), tiempoMaximo)

  let respuesta
  try {
    respuesta = await fetch(`${BASE_URL}${ruta}`, {
      method,
      headers: {
        'Content-Type': 'application/json',
        // Cada pedido lleva el token del administrador conectado.
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controlador.signal,
    })
  } catch (falla) {
    if (falla?.name === 'AbortError') throw crearError(MENSAJES.TIEMPO_AGOTADO, { codigo: 'TIEMPO_AGOTADO' })
    const sinInternet = typeof navigator !== 'undefined' && navigator.onLine === false
    throw crearError(sinInternet ? MENSAJES.SIN_INTERNET : MENSAJES.SIN_CONEXION, { codigo: 'SIN_CONEXION' })
  } finally {
    clearTimeout(temporizador)
  }

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    // Si mandamos un token y Django responde 401, la sesión ya no sirve.
    if (respuesta.status === 401 && token) {
      borrarToken()
      window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA))
    }
    const detalle = typeof datos?.detail === 'string' ? datos.detail : null
    throw crearError(detalle || mensajePorEstado(respuesta.status), { status: respuesta.status, detalles: datos })
  }
  return datos
}
