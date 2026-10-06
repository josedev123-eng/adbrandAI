// Model: llamadas a la API de Kit de Marca (HU-11).
import { apiRequest } from '../../../shared/services/apiClient'

export const ESTILOS_VISUALES = [
  { valor: 'MINIMALISTA', nombre: 'Minimalista' },
  { valor: 'MODERNO', nombre: 'Moderno' },
  { valor: 'CLASICO', nombre: 'Clásico' },
  { valor: 'DIVERTIDO', nombre: 'Divertido' },
  { valor: 'ELEGANTE', nombre: 'Elegante' },
  { valor: 'TECNOLÓGICO', nombre: 'Tecnológico' },
  { valor: 'ARTESANAL', nombre: 'Artesanal' },
  { valor: 'CORPORATIVO', nombre: 'Corporativo' },
]

export function generarKitMarca(datos) {
  return apiRequest('/kit-marca/generar', { method: 'POST', body: datos })
}

export function obtenerKitMarca(id) {
  return apiRequest(`/kit-marca/${id}`)
}

export function listarKitsMarca() {
  return apiRequest('/kit-marca')
}