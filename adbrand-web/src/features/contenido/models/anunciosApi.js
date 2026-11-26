// Model: llamadas a la API de contenido (HU-10, HU-12).
import { apiDescargar, apiRequest, TIEMPO_IA } from '../../../shared/services/apiClient'

// Los mismos valores que el enum RedSocial de Spring.
// "detalle": qué cambia al elegir la red (largo del texto, hashtags y formato de la imagen), para que se entienda.
// "formato": el formato de imagen con que se abre "Listo para publicar".
export const REDES = [
  {
    valor: 'INSTAGRAM',
    nombre: 'Instagram',
    detalle: 'Texto de hasta 80 palabras con 4 hashtags e imagen cuadrada para tu muro.',
    formato: 'CUADRADO',
  },
  {
    valor: 'FACEBOOK',
    nombre: 'Facebook',
    detalle: 'Texto más largo (hasta 120 palabras) con 2 hashtags e imagen horizontal, como se ven mejor en Facebook.',
    formato: 'HORIZONTAL',
  },
  {
    valor: 'WHATSAPP',
    nombre: 'Estados de WhatsApp',
    detalle: 'Texto muy corto (hasta 45 palabras), 1 hashtag e imagen vertical, como se ven los estados.',
    formato: 'VERTICAL',
  },
]

export const NOMBRES_TONO = {
  CERCANO: 'Cercano',
  PROFESIONAL: 'Profesional',
  DIVERTIDO: 'Divertido',
  ELEGANTE: 'Elegante',
}

export function generarAnuncio(datos) {
  return apiRequest('/contenido/anuncios', { method: 'POST', body: datos, tiempoMaximo: TIEMPO_IA })
}

export function regenerarAnuncio(id) {
  return apiRequest(`/contenido/anuncios/${id}/regenerar`, { method: 'POST', tiempoMaximo: TIEMPO_IA })
}

// "Generar → Elegir": 4 opciones de anuncio, cada una con su texto y su imagen.
// Devuelve { generacionId, oferta, redSocial, opciones: [{ id, numero, estilo, texto, imagen, proveedorImagen,
// imagenSimulada, textoSimulado, elegida }] }. "imagen" ya viene lista para <img src>.
export function generarOpciones(datos) {
  return apiRequest('/contenido/anuncios/opciones', { method: 'POST', body: datos, tiempoMaximo: TIEMPO_IA })
}

// Guarda la opción elegida como anuncio (pasa por el filtro). Devuelve { anuncio, opcion }.
export function elegirOpcion(opcionId) {
  return apiRequest(`/contenido/anuncios/opciones/${opcionId}/elegir`, { method: 'POST' })
}

// "Retocar con prompts cortos" sobre la opción elegida. Devuelve el historial:
// { opcionId, versionActual, imagenActual, versiones: [{ numero, instruccion, imagen, proveedorImagen, imagenSimulada, actual }] }.
export function retocarImagen(opcionId, instruccion) {
  return apiRequest(`/contenido/anuncios/opciones/${opcionId}/retocar`, { method: 'POST', body: { instruccion }, tiempoMaximo: TIEMPO_IA })
}

// Deshacer: volver a usar una versión anterior. Devuelve el mismo historial.
export function usarVersion(opcionId, numero) {
  return apiRequest(`/contenido/anuncios/opciones/${opcionId}/versiones/${numero}/usar`, { method: 'POST' })
}

// Listo para publicar: texto con hashtags (marca + rubro + Perú) y enlace "Pedir por WhatsApp" si hay celular.
export function prepararPublicacion(id) {
  return apiRequest(`/contenido/anuncios/${id}/publicar`)
}

// HU 65: video corto del anuncio (MP4 vertical de 8 s) para Reels, TikTok o Estados de WhatsApp. Tarda unos segundos.
export function descargarVideoAnuncio(id) {
  return apiDescargar(`/contenido/anuncios/${id}/video`, { tiempoMaximo: TIEMPO_IA })
}

// HU 66: video con IA (plan Premium, 3 al mes). La IA anima la foto del anuncio; tarda 1 a 3 minutos.
// disponibilidad: { disponible, motivo ('PLAN' | 'LIMITE' | null), usados, limite, modelo, costoEstimado }
export function disponibilidadVideoIa() {
  return apiRequest('/contenido/video-ia/disponibilidad')
}

// Devuelve { id, estado: 'CREANDO' | 'LISTO' | 'FALLIDO', progreso, error, usados, limite }.
export function iniciarVideoIa(anuncioId) {
  return apiRequest(`/contenido/anuncios/${anuncioId}/video-ia`, { method: 'POST' })
}

export function consultarVideoIa(videoId) {
  return apiRequest(`/contenido/video-ia/${videoId}`)
}

export function descargarVideoIa(videoId) {
  return apiDescargar(`/contenido/video-ia/${videoId}/archivo`)
}

// HU 61: la imagen final del anuncio (JPG) con el logo, los colores y el eslogan del kit.
// formato: CUADRADO (publicación), VERTICAL (historia o estado) u HORIZONTAL.
// HU 64: "condiciones" ({ hasta: 'AAAA-MM-DD', stock: '50' }) agrega la letra chica de la promoción abajo del diseño.
export function descargarImagenFinal(id, formato = 'CUADRADO', condiciones = {}) {
  const parametros = new URLSearchParams({ formato: formato.toLowerCase() })
  if (condiciones.hasta) parametros.set('hasta', condiciones.hasta)
  if (condiciones.stock) parametros.set('stock', condiciones.stock)
  return apiDescargar(`/contenido/anuncios/${id}/imagen-final?${parametros}`)
}
