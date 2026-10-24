// Model: llamadas a la API del consumo de tokens de IA (HU-17).
import { apiRequest } from '../../../shared/services/apiClient'

// Devuelve { mes, total, empresas: [{ usuario_id, empresa, tokens, generaciones, ultima_generacion,
// limite, porcentaje_aviso, porcentaje, alerta }], alertas: [{ usuario_id, empresa, tokens, limite, porcentaje }] }.
// limite y porcentaje vienen en null si no hay límite configurado (HU-19).
export function obtenerConsumoActual() {
  return apiRequest('/tokens/consumo-actual/')
}

// HU-20: tokens de cada mes del periodo y el total. desde y hasta van como "AAAA-MM".
// Devuelve { desde, hasta, total, meses: [{ mes, tokens, generaciones }] }.
export function obtenerHistorial(desde, hasta) {
  const parametros = new URLSearchParams({ desde, hasta })
  return apiRequest(`/tokens/historial/?${parametros}`)
}

const FORMATO_TOKENS = new Intl.NumberFormat('es-PE')
const NOMBRE_MES = new Intl.DateTimeFormat('es-PE', { month: 'long' })
const FORMATO_PORCENTAJE = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 1 })
const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
const FORMATO_MES = new Intl.DateTimeFormat('es-PE', { month: 'long', year: 'numeric' })

export function formatearTokens(numero) {
  return FORMATO_TOKENS.format(numero)
}

// HU-19: 80 → "80", 85.5 → "85.5" (sin el signo %, la vista lo agrega).
export function formatearPorcentaje(numero) {
  return FORMATO_PORCENTAJE.format(numero)
}

export function formatearFecha(texto) {
  return FORMATO_FECHA.format(new Date(texto))
}

// "2026-10" → "octubre de 2026"
export function formatearMes(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  return FORMATO_MES.format(new Date(anio, numero - 1, 1))
}

// HU-20: "2026-10" → "octubre 2026"
export function nombreDelMes(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  return `${NOMBRE_MES.format(new Date(anio, numero - 1, 1))} ${anio}`
}
