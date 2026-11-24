// Model: llamadas a la API de pagos y planes (HU 25 a 28).
import { apiRequest } from '../../../shared/services/apiClient'
import { conCache, UN_MINUTO } from '../../../shared/services/cacheConsultas'

// Los planes casi nunca cambian: se guardan 10 minutos para no pedirlos en cada pantalla.
export function obtenerPlanes() {
  return conCache('planes', () => apiRequest('/planes'), { duracion: 10 * UN_MINUTO })
}

export function pagarConTarjeta(datos) {
  return apiRequest('/pagos/tarjeta', { method: 'POST', body: datos })
}

// HU 26 con Mercado Pago: qué pasarela usar (y su llave pública) y el cobro con el token de la tarjeta.
export function obtenerConfiguracionPago() {
  return apiRequest('/pagos/configuracion')
}

export function pagarConMercadoPago(datos) {
  return apiRequest('/pagos/mercadopago', { method: 'POST', body: datos })
}

// HU 27 con Mercado Pago: Yape con el token creado a partir del celular y el código de aprobación.
export function pagarYapeConMercadoPago(datos) {
  return apiRequest('/pagos/mercadopago/yape', { method: 'POST', body: datos })
}

// HU 27: genera el QR de Yape (operación pendiente) y luego la confirma.
export function registrarYape(datos) {
  return apiRequest('/pagos/yape', { method: 'POST', body: datos })
}

export function confirmarYape(operacionId) {
  return apiRequest(`/pagos/yape/${operacionId}/confirmar`, { method: 'POST' })
}

export function obtenerYapePendientes() {
  return apiRequest('/pagos/yape/pendientes')
}

// HU 28: comprobante del pago. La primera vez el backend genera el número y envía el correo.
export function obtenerComprobante(pagoId) {
  return apiRequest(`/pagos/${pagoId}/comprobante`)
}

// Cuánto usó la empresa de su límite de tokens de IA este mes (plan pagado, gratuito o personalizado).
export function obtenerConsumoMes() {
  return apiRequest('/consumo/mes')
}
