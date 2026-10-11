// Model: llamadas a la API de suscripciones (HU-08).
import { apiRequest } from '../../../shared/services/apiClient'

export const ESTADOS_SUSCRIPCION = {
  ACTIVA: 'Activa',
  VENCIDA: 'Vencida',
  PENDIENTE_PAGO: 'Pendiente de pago',
  CANCELADA: 'Cancelada',
}

// Estados que el endpoint /con-deuda/ sabe filtrar.
export const ESTADOS_CON_DEUDA = ['VENCIDA', 'PENDIENTE_PAGO']

export function listarSuscripciones() {
  return apiRequest('/suscripciones/con-deuda/')
}

export function listarSuscripcionesPorEstado(estado) {
  if (!estado) return listarSuscripciones()
  return apiRequest(`/suscripciones/con-deuda/?estado=${estado}`)
}

// HU-05: responde { total } con las suscripciones activas y vigentes hoy.
export function obtenerSuscripcionesActivas() {
  return apiRequest('/suscripciones/activas/')
}

// HU-06: responde [{ mes: 'AAAA-MM', total: '150.00' }, ...] del mes más antiguo al más reciente.
export function obtenerIngresosMensuales() {
  return apiRequest('/suscripciones/ingresos-mensuales/')
}

const FORMATO_MES = new Intl.DateTimeFormat('es-PE', { month: 'long', year: 'numeric' })

// '2026-09' se muestra como 'setiembre de 2026'.
export function formatearMes(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  return FORMATO_MES.format(new Date(anio, numero - 1, 1))
}

export function formatearSoles(valor) {
  const numero = Number(valor).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
  return `S/ ${numero}`
}