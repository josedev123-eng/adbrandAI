// Model: el formulario de Contacto (soporte). Se puede enviar con o sin sesión.
import { apiRequest } from '../../../shared/services/apiClient'

// Los mismos valores que acepta Spring (mensaje_contacto.asunto).
export const MOTIVOS = [
  { valor: 'DUDA', nombre: 'Tengo una duda' },
  { valor: 'PROBLEMA', nombre: 'Algo no funciona' },
  { valor: 'PAGOS', nombre: 'Pagos y planes' },
  { valor: 'SUGERENCIA', nombre: 'Sugerencia' },
  { valor: 'OTRO', nombre: 'Otro' },
]

export const MAXIMO_MENSAJE = 2000

export function enviarMensajeContacto(datos) {
  return apiRequest('/contacto', { method: 'POST', body: datos })
}
