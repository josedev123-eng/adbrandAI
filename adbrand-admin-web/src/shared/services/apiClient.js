// Cliente HTTP único hacia el backend de administración (Django).
const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8000/api'

export async function apiRequest(ruta, { method = 'GET', body } = {}) {
  const respuesta = await fetch(`${BASE_URL}${ruta}`, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: body ? JSON.stringify(body) : undefined,
  })

  const datos = await respuesta.json().catch(() => null)

  if (!respuesta.ok) {
    // Django devuelve { campo: ["mensaje"] } cuando falla una validación
    const error = new Error('La solicitud no se pudo completar')
    error.status = respuesta.status
    error.detalles = datos
    throw error
  }
  return datos
}