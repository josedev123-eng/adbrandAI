// Cliente HTTP único hacia el backend de administración (Django).
import { borrarToken, leerToken } from './sesionToken'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8000/api'

// Aviso para toda la web: el token venció o es falso, hay que volver al login.
export const EVENTO_SESION_VENCIDA = 'adbrand:sesion-vencida'

export async function apiRequest(ruta, { method = 'GET', body, headers = {} } = {}) {
  const token = leerToken()
  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      // Cada pedido lleva el token del administrador conectado.
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...headers,
    },
    body: body ? JSON.stringify(body) : undefined,
  })

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    // Si mandamos un token y Django responde 401, la sesión ya no sirve.
    if (respuesta.status === 401 && token) {
      borrarToken()
      window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA))
    }
    // Django devuelve { campo: ["mensaje"] } cuando falla una validación
    const error = new Error('La solicitud no se pudo completar')
    error.status = respuesta.status
    error.detalles = datos
    throw error
  }
  return datos
}
