// Model: llamadas a la API del consumo de tokens de IA (HU-17).
import { apiRequest } from '../../../shared/services/apiClient'

// Devuelve { mes, total, empresas: [{ usuario_id, empresa, tokens, generaciones, ultima_generacion }] }.
export function obtenerConsumoActual() {
  return apiRequest('/tokens/consumo-actual/')
}

const FORMATO_TOKENS = new Intl.NumberFormat('es-PE')
const FORMATO_FECHA = new Intl.DateTimeFormat('es-PE', { dateStyle: 'medium', timeStyle: 'short' })
const FORMATO_MES = new Intl.DateTimeFormat('es-PE', { month: 'long', year: 'numeric' })

export function formatearTokens(numero) {
  return FORMATO_TOKENS.format(numero)
}

export function formatearFecha(texto) {
  return FORMATO_FECHA.format(new Date(texto))
}

// "2026-10" → "octubre de 2026"
export function formatearMes(mes) {
  const [anio, numero] = mes.split('-').map(Number)
  return FORMATO_MES.format(new Date(anio, numero - 1, 1))
}
