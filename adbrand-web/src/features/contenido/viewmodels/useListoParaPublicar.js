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
  // "Subir a mi estado": se pide el anuncio en vertical y se abre el menú del celular (WhatsApp → Mi estado).
  const [subiendoEstado, setSubiendoEstado] = useState(false)
  const [avisoEstado, setAvisoEstado] = useState(null)
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

  // Abre WhatsApp con el texto listo para enviar a un contacto o a un grupo. En la computadora va directo a
  // WhatsApp Web (wa.me pasaba por una página que intentaba abrir la app de escritorio y dejaba la web trabada);
  // en el celular abre la app. Se usa un enlace en vez de window.open para no bloquear la pestaña.
  function compartirWhatsapp() {
    if (!publicacion) return
    const texto = encodeURIComponent(textoFinal)
    const enCelular = /Android|iPhone|iPad|iPod/i.test(navigator.userAgent)
    const enlace = document.createElement('a')
    enlace.href = enCelular ? `https://wa.me/?text=${texto}` : `https://web.whatsapp.com/send?text=${texto}`
    enlace.target = '_blank'
    enlace.rel = 'noopener noreferrer'
    enlace.click()
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
    const imagen = await descargarImagenFinal(anuncioId, 'CUADRADO', condiciones).then(comoDataUrl).catch(() => null)
    ventana.document.open()
    // Con un plan de pago el afiche sale sin "Hecho con AdBrand.AI".
    const conMarcaAdbrand = publicacion.videoDisponible === false
    ventana.document.write(
      htmlAfiche({
        marca: publicacion.marca ?? {},
        imagen,
        // HU 62: el titular arriba y, abajo, el resto del texto (sin repetir el titular).
        titular: publicacion.titular,
        oferta: publicacion.ofertaDestacada,
        vigencia: vigenciaDe(condiciones),
        texto: sinTitular(textoSinEnlace(publicacion.textoParaPublicar), publicacion.titular),
        qr,
        telefono: telefonoDe(publicacion.enlaceWhatsapp),
        conMarcaAdbrand,
      }),
    )
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

  // WhatsApp no deja que una web publique el estado sola: en el celular se abre el menú para compartir con la
  // imagen vertical (ahí eliges WhatsApp → "Mi estado"); en la computadora se descarga para subirla desde el celular.
  async function subirAEstado() {
    setError(null)
    setAvisoEstado(null)
    setSubiendoEstado(true)
    try {
      const blob = await descargarImagenFinal(anuncioId, 'VERTICAL', condiciones)
      const archivo = new File([blob], `estado-${anuncioId}.jpg`, { type: 'image/jpeg' })
      if (navigator.canShare?.({ files: [archivo] })) {
        try {
          await navigator.clipboard?.writeText(textoFinal)
        } catch {
          // Si no se puede copiar el texto, igual se comparte la imagen.
        }
        try {
          await navigator.share({ files: [archivo] })
          setAvisoEstado('Elige WhatsApp y luego "Mi estado". El texto con tus hashtags ya está copiado.')
        } catch {
          // Cerró el menú de compartir: no es un error.
        }
        return
      }
      const url = URL.createObjectURL(blob)
      const enlace = document.createElement('a')
      enlace.href = url
      enlace.download = archivo.name
      enlace.click()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
      setAvisoEstado(
        'Descargamos tu anuncio en vertical. Pásalo a tu celular y en WhatsApp ve a Novedades → "Añadir estado". Desde el celular, este botón abre WhatsApp directo.',
      )
    } catch (e) {
      setError(e?.message || 'No se pudo preparar tu estado.')
    } finally {
      setSubiendoEstado(false)
    }
  }

  return {
    publicacion,
    textoFinal,
    subiendoEstado,
    avisoEstado,
    subirAEstado,
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

// HU 62: "Válido hasta el 24/12/2026 · Stock: 50 unidades" para el afiche (vacío si no se puso nada).
export function vigenciaDe({ hasta, stock } = {}) {
  const partes = []
  if (hasta) {
    const [anio, mes, dia] = hasta.split('-')
    partes.push(`Válido hasta el ${dia}/${mes}/${anio}`)
  }
  if (stock && Number(stock) > 0) partes.push(`Stock: ${Number(stock)} unidades`)
  return partes.join(' · ')
}

// El número del WhatsApp a la vista: "https://wa.me/51987654321?text=…" → "+51 987 654 321".
export function telefonoDe(enlace) {
  const numero = /wa\.me\/(\d+)/.exec(enlace ?? '')?.[1]
  if (!numero) return ''
  const local = numero.startsWith('51') && numero.length === 11 ? numero.slice(2) : numero
  const prefijo = local === numero ? '+' : '+51 '
  return prefijo + local.replace(/(\d{3})(?=\d)/g, '$1 ').trim()
}

// El texto de abajo no repite el titular: se quita la primera frase si es la del titular. Y va corto (cabe en A4).
export function sinTitular(texto, titular) {
  let resto = (texto ?? '').trim()
  const clave = (titular ?? '').replace(/…$/, '').trim().toLowerCase().slice(0, 20)
  const primera = resto.match(/^[^.!?\n]*[.!?]?/)?.[0] ?? ''
  if (clave && primera.toLowerCase().replace(/^[^\p{L}\d]+/u, '').startsWith(clave)) resto = resto.slice(primera.length).trim()
  if (resto.length > 220) resto = `${resto.slice(0, resto.lastIndexOf(' ', 217)).replace(/[,;:]$/, '')}…`
  return resto
}

// Afiche A4 (HU 62), siempre en una sola hoja sin cortes: arriba el logo, el nombre grande y el eslogan; la oferta
// destacada y su vigencia; la imagen con el titular; un resumen corto; y el QR "Escanea y pide por WhatsApp" con el
// número a la vista. Con los colores del kit. "conMarcaAdbrand": solo con el plan gratis lleva "Hecho con AdBrand.AI".
export function htmlAfiche({ marca, imagen, titular, oferta, vigencia, texto, qr, telefono, conMarcaAdbrand = true }) {
  const principal = colorSeguro(marca.colorPrincipal, '#4f46e5')
  const acento = colorSeguro(marca.colorAcento, '#f97316')
  const logo = /^data:image\//.test(marca.logo ?? '') ? marca.logo : null
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Afiche - ${escapar(marca.nombre)}</title>
<style>
  @page { size: A4 portrait; margin: 0; }
  * { box-sizing: border-box; -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  html, body { margin: 0; padding: 0; width: 210mm; height: 297mm; overflow: hidden; }
  body { font-family: 'Segoe UI', Arial, sans-serif; color: #1b1d2e; background: #fff; }
  .hoja { width: 210mm; height: 297mm; padding: 10mm 12mm 8mm; display: flex; flex-direction: column; gap: 5mm; overflow: hidden; break-inside: avoid; page-break-after: avoid; }
  .cabecera { display: flex; align-items: center; justify-content: center; gap: 5mm; text-align: left; }
  .cabecera img { width: 28mm; height: 28mm; object-fit: contain; flex-shrink: 0; }
  h1 { font-size: 34pt; line-height: 1; margin: 0; color: ${principal}; letter-spacing: -0.5pt; }
  .eslogan { font-style: italic; font-size: 14pt; margin: 2mm 0 0; color: #4a4d63; }
  .oferta { background: ${principal}; color: #fff; border-radius: 6mm; padding: 5mm 8mm; text-align: center; border-bottom: 2.5mm solid ${acento}; }
  .oferta strong { display: block; font-size: 36pt; line-height: 1.05; font-weight: 900; text-transform: uppercase; }
  .oferta span { display: block; font-size: 13pt; margin-top: 2mm; opacity: 0.95; }
  .foto { position: relative; flex: 1 1 auto; min-height: 0; border-radius: 6mm; overflow: hidden; background: color-mix(in srgb, ${principal} 12%, #fff); }
  .foto img { width: 100%; height: 100%; object-fit: cover; display: block; }
  .foto h2 { position: absolute; left: 0; right: 0; bottom: 0; margin: 0; padding: 14mm 7mm 5mm; font-size: 22pt; line-height: 1.15; color: #fff; background: linear-gradient(transparent, rgba(0,0,0,0.72)); }
  .foto.sin-imagen h2 { position: static; color: ${principal}; background: none; padding: 8mm; font-size: 26pt; }
  .texto { font-size: 13pt; line-height: 1.35; margin: 0; text-align: center; color: #33364a; }
  .pedido { display: flex; align-items: center; gap: 7mm; padding: 4mm 7mm; border: 1mm solid ${acento}; border-radius: 6mm; background: color-mix(in srgb, ${acento} 10%, #fff); flex-shrink: 0; }
  .pedido img { width: 42mm; height: 42mm; flex-shrink: 0; }
  .pedido p { margin: 0; font-size: 21pt; font-weight: 800; line-height: 1.15; color: ${principal}; }
  .pedido .numero { display: block; margin-top: 2mm; font-size: 17pt; font-weight: 700; color: #1b1d2e; letter-spacing: 0.5pt; }
  .pie { color: #8a8da3; font-size: 8pt; text-align: center; margin: -2mm 0 0; flex-shrink: 0; }
</style></head><body><div class="hoja">
  <header class="cabecera">${logo ? `<img src="${logo}" alt="Logo">` : ''}<div><h1>${escapar(marca.nombre)}</h1>${
    marca.eslogan ? `<p class="eslogan">“${escapar(marca.eslogan)}”</p>` : ''
  }</div></header>
  ${oferta || vigencia ? `<section class="oferta">${oferta ? `<strong>${escapar(oferta)}</strong>` : ''}${vigencia ? `<span>${escapar(vigencia)}</span>` : ''}</section>` : ''}
  <section class="foto${imagen ? '' : ' sin-imagen'}">${imagen ? `<img src="${imagen}" alt="">` : ''}${titular ? `<h2>${escapar(titular)}</h2>` : ''}</section>
  ${texto ? `<p class="texto">${escapar(texto)}</p>` : ''}
  <div class="pedido"><img src="${qr}" alt="QR para pedir por WhatsApp"><p>Escanea y pide<br>por WhatsApp${
    telefono ? `<span class="numero">📲 ${escapar(telefono)}</span>` : ''
  }</p></div>
  ${conMarcaAdbrand ? '<p class="pie">Hecho con AdBrand.AI</p>' : ''}
</div></body></html>`
}
