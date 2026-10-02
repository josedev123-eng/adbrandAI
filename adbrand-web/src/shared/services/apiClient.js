// Cliente HTTP único hacia el backend del usuario (Spring Boot).
const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080/api'

export async function apiRequest(ruta, { method = 'GET', body } = {}) {
  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    // Spring devuelve { codigo, mensaje, campos } (ver shared/error/ErrorResponse.java)
    const error = new Error(datos?.mensaje ?? 'La solicitud no se pudo completar')
    error.status = respuesta.status
    error.codigo = datos?.codigo
    error.campos = datos?.campos ?? {}
    throw error
  }
  return datos
}