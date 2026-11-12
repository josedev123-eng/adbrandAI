// Model: llamadas a la API de contenido (HU-10, HU-12).
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

export function regenerarAnuncio(id) {
  return apiRequest(`/contenido/anuncios/${id}/regenerar`, { method: 'POST' })
}

// "Generar → Elegir": 4 opciones de anuncio, cada una con su texto y su imagen.
// Devuelve { generacionId, oferta, redSocial, opciones: [{ id, numero, estilo, texto, imagen, proveedorImagen,
// imagenSimulada, textoSimulado, elegida }] }. "imagen" ya viene lista para <img src>.
export function generarOpciones(datos) {
  return apiRequest('/contenido/anuncios/opciones', { method: 'POST', body: datos })
}

// Guarda la opción elegida como anuncio (pasa por el filtro). Devuelve { anuncio, opcion }.
export function elegirOpcion(opcionId) {
  return apiRequest(`/contenido/anuncios/opciones/${opcionId}/elegir`, { method: 'POST' })
}

// "Retocar con prompts cortos" sobre la opción elegida. Devuelve el historial:
// { opcionId, versionActual, imagenActual, versiones: [{ numero, instruccion, imagen, proveedorImagen, imagenSimulada, actual }] }.
export function retocarImagen(opcionId, instruccion) {
  return apiRequest(`/contenido/anuncios/opciones/${opcionId}/retocar`, { method: 'POST', body: { instruccion } })
}

// Deshacer: volver a usar una versión anterior. Devuelve el mismo historial.
export function usarVersion(opcionId, numero) {
  return apiRequest(`/contenido/anuncios/opciones/${opcionId}/versiones/${numero}/usar`, { method: 'POST' })
}