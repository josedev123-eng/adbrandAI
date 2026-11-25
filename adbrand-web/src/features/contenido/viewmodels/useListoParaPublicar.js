// ViewModel: "Listo para publicar". Pide a Spring el texto con hashtags y el enlace "Pedir por WhatsApp",
// y arma el afiche para imprimir con un código QR que lleva a ese WhatsApp.
import QRCode from 'qrcode'
import { useState } from 'react'
import { descargarImagenFinal, descargarVideoAnuncio, prepararPublicacion } from '../models/anunciosApi'
import { comoDataUrl } from './useImagenConMarca'

export function useListoParaPublicar(anuncioId) {
  const [publicacion, setPublicacion] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)
  const [copiado, setCopiado] = useState(false)
  // HU 65: el video se arma en el servidor (unos segundos).
  const [creandoVideo, setCreandoVideo] = useState(false)
  // HU 64: lo que el dueño completa para cumplir las reglas (hasta cuándo vale y cuántas unidades hay).
  const [condiciones, setCondiciones] = useState({ hasta: '', stock: '' })
  const textoFinal = publicacion ? conCondiciones(publicacion.textoParaPublicar, condiciones) : ''

  function cambiarCondicion(campo, valor) {
    setCondiciones((actuales) => ({ ...actuales, [campo]: valor }))
  }

  async function abrir() {
    setError(null)
    setCargando(true)
    try {
      setPublicacion(await prepararPublicacion(anuncioId))
    } catch (e) {
      setError(e?.message || 'No se pudo preparar el anuncio.')
    } finally {
      setCargando(false)
    }
  }

  async function copiar() {
    if (!publicacion) return
    try {
      await navigator.clipboard.writeText(textoFinal)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2000)
    } catch {
      setError('No se pudo copiar. Selecciona el texto y cópialo a mano.')
    }
  }

  // Abre WhatsApp (web o app) con el texto listo para enviar a un contacto o a un grupo.
  function compartirWhatsapp() {
    if (!publicacion) return
    window.open(`https://wa.me/?text=${encodeURIComponent(textoFinal)}`, '_blank', 'noopener')
  }

  // Afiche A4 para pegar en la puerta o el mostrador: imagen, texto y un QR que abre el WhatsApp del negocio.
  // HU 61: con el logo, los colores y el eslogan del kit, y la imagen final que ya lleva la marca.
  async function imprimirAfiche() {
    if (!publicacion?.enlaceWhatsapp) return
    // La ventana se abre antes de esperar la imagen para que el navegador no la bloquee.
    const ventana = window.open('', '_blank')
    if (!ventana) {
      setError('Tu navegador bloqueó la ventana. Permite las ventanas emergentes para imprimir el afiche.')
      return
    }
    ventana.document.write('<p style="font-family:sans-serif">Preparando tu afiche…</p>')
    const qr = await QRCode.toDataURL(publicacion.enlaceWhatsapp, { width: 360, margin: 1 })
    const imagen = await descargarImagenFinal(anuncioId, 'CUADRADO').then(comoDataUrl).catch(() => null)
    ventana.document.open()
    ventana.document.write(htmlAfiche({ marca: publicacion.marca ?? {}, imagen, texto: textoSinEnlace(textoFinal), qr }))
    ventana.document.close()
    ventana.onload = () => ventana.print()
  }

  // HU 65: descarga el video vertical del anuncio (con su marca y su WhatsApp) para subirlo a Reels o Estados.
  async function descargarVideo() {
    setError(null)
    setCreandoVideo(true)
    try {
      const blob = await descargarVideoAnuncio(anuncioId)
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = `anuncio-${anuncioId}.mp4`
      enlace.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (e) {
      setError(e?.message || 'No se pudo crear el video.')
    } finally {
      setCreandoVideo(false)
    }
  }

  return {
    publicacion,
    textoFinal,
    creandoVideo,
    descargarVideo,
    condiciones,
    cambiarCondicion,
    cargando,
    error,
    copiado,
    abrir,
    copiar,
    compartirWhatsapp,
    imprimirAfiche,
  }
}

// HU 64: agrega "Válido hasta el 24/12/2026. Stock: 50 unidades." después del texto del anuncio
// (antes del enlace de WhatsApp y de los hashtags).
export function conCondiciones(texto, { hasta, stock }) {
  const partes = []
  if (hasta) {
    const [anio, mes, dia] = hasta.split('-')
    partes.push(`Válido hasta el ${dia}/${mes}/${anio}`)
  }
  if (stock && Number(stock) > 0) partes.push(`${partes.length ? 'o hasta agotar stock' : 'Hasta agotar stock'} (${Number(stock)} unidades)`)
  if (partes.length === 0) return texto
  const linea = `${partes.join(' ')}.`
  // HU 61: también antes de la firma con el eslogan ("— Panadería Rosa: “…”").
  const corte = texto.search(/\n\n(— |📲|#)/)
  return corte === -1 ? `${texto}\n\n${linea}` : `${texto.slice(0, corte)}\n\n${linea}${texto.slice(corte)}`
}

// Para el afiche: sin el enlace, los hashtags ni la firma (el afiche ya muestra el logo y el eslogan).
function textoSinEnlace(texto) {
  return texto
    .split('\n📲')[0]
    .split('\n')
    .filter((linea) => !linea.startsWith('— '))
    .join('\n')
    .trim()
}

// Solo colores "#RRGGBB" (vienen del kit); si no, los de AdBrand.
function colorSeguro(valor, porDefecto) {
  return /^#[0-9a-f]{6}$/i.test(valor ?? '') ? valor : porDefecto
}

function escapar(texto) {
  return String(texto ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])
}

export function htmlAfiche({ marca, imagen, texto, qr }) {
  const principal = colorSeguro(marca.colorPrincipal, '#4f46e5')
  const acento = colorSeguro(marca.colorAcento, '#f97316')
  const logo = /^data:image\//.test(marca.logo ?? '') ? marca.logo : null
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Afiche - ${escapar(marca.nombre)}</title>
<style>
  @page { size: A4; margin: 14mm; }
  * { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  body { font-family: 'Segoe UI', Arial, sans-serif; color: #1b1d2e; margin: 0; text-align: center; }
  .cabecera { display: flex; align-items: center; justify-content: center; gap: 5mm; padding-bottom: 4mm; border-bottom: 2mm solid ${acento}; margin-bottom: 5mm; }
  .cabecera img { width: 22mm; height: 22mm; object-fit: contain; border-radius: 50%; }
  h1 { font-size: 30pt; margin: 0; color: ${principal}; }
  .eslogan { font-style: italic; color: ${principal}; font-size: 15pt; margin: 0 0 5mm; }
  img.anuncio { width: 100%; max-height: 120mm; object-fit: contain; border-radius: 6mm; }
  .texto { font-size: 15pt; line-height: 1.4; margin: 6mm 0; white-space: pre-line; }
  .pedido { display: flex; align-items: center; justify-content: center; gap: 8mm; margin-top: 4mm; padding: 4mm; border: 1mm solid ${principal}; border-radius: 6mm; }
  .pedido img { width: 45mm; height: 45mm; }
  .pedido p { font-size: 18pt; font-weight: 700; margin: 0; text-align: left; color: ${principal}; }
  .pie { color: #5d6178; font-size: 9pt; margin-top: 6mm; }
</style></head><body>
  <div class="cabecera">${logo ? `<img src="${logo}" alt="Logo">` : ''}<h1>${escapar(marca.nombre)}</h1></div>
  ${marca.eslogan ? `<p class="eslogan">“${escapar(marca.eslogan)}”</p>` : ''}
  ${imagen ? `<img class="anuncio" src="${imagen}" alt="">` : ''}
  <p class="texto">${escapar(texto)}</p>
  <div class="pedido"><img src="${qr}" alt="QR para pedir por WhatsApp"><p>Escanea y pide<br>por WhatsApp</p></div>
  <p class="pie">Hecho con AdBrand.AI</p>
</body></html>`
}
