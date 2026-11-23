// ViewModel: "Listo para publicar". Pide a Spring el texto con hashtags y el enlace "Pedir por WhatsApp",
// y arma el afiche para imprimir con un código QR que lleva a ese WhatsApp.
import QRCode from 'qrcode'
import { useState } from 'react'
import { prepararPublicacion } from '../models/anunciosApi'

export function useListoParaPublicar(anuncioId) {
  const [publicacion, setPublicacion] = useState(null)
  const [cargando, setCargando] = useState(false)
  const [error, setError] = useState(null)
  const [copiado, setCopiado] = useState(false)

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
      await navigator.clipboard.writeText(publicacion.textoParaPublicar)
      setCopiado(true)
      setTimeout(() => setCopiado(false), 2000)
    } catch {
      setError('No se pudo copiar. Selecciona el texto y cópialo a mano.')
    }
  }

  // Abre WhatsApp (web o app) con el texto listo para enviar a un contacto o a un grupo.
  function compartirWhatsapp() {
    if (!publicacion) return
    window.open(`https://wa.me/?text=${encodeURIComponent(publicacion.textoParaPublicar)}`, '_blank', 'noopener')
  }

  // Afiche A4 para pegar en la puerta o el mostrador: imagen, texto, eslogan y un QR que abre el WhatsApp del negocio.
  async function imprimirAfiche({ imagen, nombre, eslogan }) {
    if (!publicacion?.enlaceWhatsapp) return
    const qr = await QRCode.toDataURL(publicacion.enlaceWhatsapp, { width: 360, margin: 1 })
    const ventana = window.open('', '_blank')
    if (!ventana) {
      setError('Tu navegador bloqueó la ventana. Permite las ventanas emergentes para imprimir el afiche.')
      return
    }
    ventana.document.write(htmlAfiche({ imagen, nombre, eslogan, texto: textoSinEnlace(publicacion), qr }))
    ventana.document.close()
    ventana.onload = () => ventana.print()
  }

  return { publicacion, cargando, error, copiado, abrir, copiar, compartirWhatsapp, imprimirAfiche }
}

function textoSinEnlace(publicacion) {
  return publicacion.textoParaPublicar.split('\n📲')[0].trim()
}

function escapar(texto) {
  return String(texto ?? '').replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' })[c])
}

function htmlAfiche({ imagen, nombre, eslogan, texto, qr }) {
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><title>Afiche - ${escapar(nombre)}</title>
<style>
  @page { size: A4; margin: 14mm; }
  body { font-family: 'Segoe UI', Arial, sans-serif; color: #1b1d2e; margin: 0; text-align: center; }
  h1 { font-size: 30pt; margin: 0 0 4mm; }
  .eslogan { font-style: italic; color: #4f46e5; font-size: 15pt; margin: 0 0 6mm; }
  img.anuncio { width: 100%; max-height: 120mm; object-fit: cover; border-radius: 6mm; }
  .texto { font-size: 15pt; line-height: 1.4; margin: 6mm 0; white-space: pre-line; }
  .pedido { display: flex; align-items: center; justify-content: center; gap: 8mm; margin-top: 4mm; }
  .pedido img { width: 45mm; height: 45mm; }
  .pedido p { font-size: 18pt; font-weight: 700; margin: 0; text-align: left; }
  .pie { color: #5d6178; font-size: 9pt; margin-top: 6mm; }
</style></head><body>
  <h1>${escapar(nombre)}</h1>
  ${eslogan ? `<p class="eslogan">“${escapar(eslogan)}”</p>` : ''}
  ${imagen ? `<img class="anuncio" src="${imagen}" alt="">` : ''}
  <p class="texto">${escapar(texto)}</p>
  <div class="pedido"><img src="${qr}" alt="QR para pedir por WhatsApp"><p>Escanea y pide<br>por WhatsApp</p></div>
  <p class="pie">Hecho con AdBrand.AI</p>
</body></html>`
}
