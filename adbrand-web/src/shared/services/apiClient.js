// Cliente HTTP único hacia el backend del usuario (Spring Boot).
import { borrarToken, leerToken } from './sesionToken'

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

// HU-22: lo escucha SesionProvider para volver al inicio de sesión.
export const EVENTO_SESION_VENCIDA = 'adbrand:sesion-vencida'

export async function apiRequest(ruta, { method = 'GET', body } = {}) {
  // HU-22: si hay sesión, cada petición lleva el token.
  const token = leerToken()
  const headers = { 'Content-Type': 'application/json' }
  if (token) headers.Authorization = `Bearer ${token}`

  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    // HU-22: un 401 con token significa que la sesión venció o se alteró. El 401 del propio
    // inicio de sesión (sin token) no cierra nada: solo es una contraseña equivocada.
    if (respuesta.status === 401 && token) {
      borrarToken()
      window.dispatchEvent(new Event(EVENTO_SESION_VENCIDA))
    }

    // Spring devuelve { codigo, mensaje, campos } (ver shared/error/ErrorResponse.java)
    const error = new Error(datos?.mensaje ?? 'La solicitud no se pudo completar')
    error.status = respuesta.status
    error.codigo = datos?.codigo
    error.campos = datos?.campos ?? {}
    throw error
  }
  return datos
}
