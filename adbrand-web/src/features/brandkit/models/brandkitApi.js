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
// HU-29: guarda el logo diseñado en el editor; queda como una versión nueva del kit.
// 400 con "campos" si un color o el texto están mal; 404 si el kit no es de la empresa.
export function guardarLogoKitMarca(id, logo) {
  return apiRequest(`/kit-marca/${id}/logo`, { method: 'PUT', body: logo })
}

export const FORMAS_LOGO = [
  { valor: 'CIRCULO', nombre: 'Círculo' },
  { valor: 'CUADRADO', nombre: 'Cuadrado' },
  { valor: 'REDONDEADO', nombre: 'Redondeado' },
]

// Tipografías que la web ya carga (index.css) más dos genéricas que tiene cualquier navegador.
export const TIPOGRAFIAS_LOGO = ['Bricolage Grotesque', 'Plus Jakarta Sans', 'JetBrains Mono', 'Georgia', 'Arial']

// HU-30: guarda solo la paleta como una versión nueva del kit.
// 400 con "campos" (colores[i].hex) si un color está mal escrito, o con codigo COLOR_REPETIDO si se repite uno.
export function guardarColoresKitMarca(id, colores) {
  return apiRequest(`/kit-marca/${id}/colores`, { method: 'PUT', body: { colores } })
}

// HU-31: guarda tipografías y voz de marca como una versión nueva del kit.
// Los anuncios que se generen después escriben con esa voz. 400 con "campos" si falta algo.
export function guardarIdentidadKitMarca(id, identidad) {
  return apiRequest(`/kit-marca/${id}/identidad`, { method: 'PUT', body: identidad })
}

// HU-31: tipografías sugeridas en el editor de identidad (se puede escribir otra).
export const TIPOGRAFIAS_SUGERIDAS = [...TIPOGRAFIAS_LOGO, 'Verdana', 'Times New Roman', 'Trebuchet MS']

// HU-30: texto negro o blanco según qué se lea mejor sobre un color #RRGGBB (luminancia relativa, como en WCAG).
export function textoLegible(hex) {
  const [r, g, b] = [1, 3, 5].map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
  const canal = (c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4)
  const luminancia = 0.2126 * canal(r) + 0.7152 * canal(g) + 0.0722 * canal(b)
  return luminancia > 0.4 ? '#1A1A1A' : '#FFFFFF'
}
