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

// HU-32: guarda los cambios hechos a mano. 404 si el kit no es de la empresa; 400 si un color está mal escrito.
export function editarKitMarca(id, datos) {
  return apiRequest(`/kit-marca/${id}`, { method: 'PUT', body: datos })
}

// HU-32: historial de ediciones, la más reciente primero.
export function listarVersionesKitMarca(id) {
  return apiRequest(`/kit-marca/${id}/versiones`)
}

// La paleta se guarda como texto, una línea por color: "#2E7D32 - Verde bosque - Confianza".
export function leerPaleta(texto) {
  if (!texto) return []
  return texto
    .split('\n')
    .map((linea) => linea.trim())
    .filter(Boolean)
    .map((linea) => {
      const [hex, ...resto] = linea.split(' - ')
      return { hex: hex.trim(), nombre: resto.join(' - ').trim() }
    })
}