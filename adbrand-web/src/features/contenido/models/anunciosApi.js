// Model: llamadas a la API de contenido (HU-10).
import { apiRequest } from '../../../shared/services/apiClient'

// Los mismos valores que el enum RedSocial de Spring.
export const REDES = [
  { valor: 'INSTAGRAM', nombre: 'Instagram' },
  { valor: 'FACEBOOK', nombre: 'Facebook' },
]

export const NOMBRES_TONO = {
  CERCANO: 'Cercano',
  PROFESIONAL: 'Profesional',
  DIVERTIDO: 'Divertido',
  ELEGANTE: 'Elegante',
}

export function generarAnuncio(datos) {
  return apiRequest('/contenido/anuncios', { method: 'POST', body: datos })
}