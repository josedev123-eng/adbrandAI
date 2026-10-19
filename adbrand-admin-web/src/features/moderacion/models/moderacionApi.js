// Model: llamadas a la API de moderación (HU-14, HU-15).
import { apiRequest } from '../../../shared/services/apiClient'

export const NOMBRES_RED = { INSTAGRAM: 'Instagram', FACEBOOK: 'Facebook' }
export const NOMBRES_TONO = { CERCANO: 'Cercano', PROFESIONAL: 'Profesional', DIVERTIDO: 'Divertido', ELEGANTE: 'Elegante' }
export const NOMBRES_ESTADO = { APROBADO: 'Aprobado', DUDOSO: 'Dudoso', RECHAZADO: 'Rechazado' }

export function listarDudosos() {
  return apiRequest('/moderacion/contenidos/dudosos/')
}

export function obtenerContenido(id) {
  return apiRequest(`/moderacion/contenidos/${id}/`)
}

// El moderador no se manda: Django lo saca del token de sesión.
export function moderarContenido(id, accion, motivoRechazo = '') {
  return apiRequest(`/moderacion/contenidos/${id}/moderar/`, {
    method: 'POST',
    body: { accion, ...(motivoRechazo ? { motivo_rechazo: motivoRechazo } : {}) },
  })
}

// HU-16: contenidos aprobados y rechazados, 20 por página. Los filtros vacíos no se envían.
// Devuelve { total, pagina, paginas, resultados: [{ id, empresa, tipo, red_social, oferta, texto, estado,
// motivo, moderador, fecha_decision }] }.
export function listarHistorial({ estado, desde, hasta, pagina = 1 } = {}) {
  const parametros = new URLSearchParams()
  if (estado) parametros.set('estado', estado)
  if (desde) parametros.set('desde', desde)
  if (hasta) parametros.set('hasta', hasta)
  if (pagina > 1) parametros.set('pagina', pagina)

  const consulta = parametros.toString()
  return apiRequest(consulta ? `/moderacion/historial/?${consulta}` : '/moderacion/historial/')
}

const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })

export function formatearFecha(texto) {
  return FORMATO_FECHA.format(new Date(texto))
}

// HU-16: "2026-10-27T09:05:00" → "27/10/2026 09:05"
export function formatearFechaHora(texto) {
  const fecha = new Date(texto)
  const dos = (numero) => String(numero).padStart(2, '0')
  return `${dos(fecha.getDate())}/${dos(fecha.getMonth() + 1)}/${fecha.getFullYear()} ${dos(fecha.getHours())}:${dos(fecha.getMinutes())}`
}
