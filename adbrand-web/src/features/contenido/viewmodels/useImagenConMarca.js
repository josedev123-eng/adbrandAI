// ViewModel (HU 61): la imagen final del anuncio con el logo, los colores y el eslogan del kit.
// La arma Spring en el formato elegido; aquí se muestra, se descarga o se comparte (con el texto listo).
import { useEffect, useState } from 'react'
import { descargarImagenFinal } from '../models/anunciosApi'

export const FORMATOS_IMAGEN = [
  { valor: 'CUADRADO', nombre: 'Publicación', detalle: 'cuadrada' },
  { valor: 'VERTICAL', nombre: 'Historia o estado', detalle: 'vertical' },
  { valor: 'HORIZONTAL', nombre: 'Portada', detalle: 'horizontal' },
]

export function useImagenConMarca(anuncioId) {
  const [formato, setFormato] = useState('CUADRADO')
  // { formato, blob, url } de la última imagen que llegó, o { formato, error }.
  const [resultado, setResultado] = useState(null)
  const [aviso, setAviso] = useState(null)

  useEffect(() => {
    let vigente = true
    let url = null
    descargarImagenFinal(anuncioId, formato)
      .then((blob) => {
        if (!vigente) return
        url = URL.createObjectURL(blob)
        setResultado({ formato, blob, url })
      })
      .catch((e) => {
        if (vigente) setResultado({ formato, error: e?.message || 'No se pudo crear la imagen con tu marca.' })
      })
    return () => {
      vigente = false
      if (url) URL.revokeObjectURL(url)
    }
  }, [anuncioId, formato])

  const actual = resultado?.formato === formato ? resultado : null
  const nombreArchivo = `anuncio-${anuncioId}-${formato.toLowerCase()}.jpg`

  function elegirFormato(valor) {
    setAviso(null)
    setFormato(valor)
  }

  function descargar() {
    if (!actual?.url) return
    const enlace = document.createElement('a')
    enlace.href = actual.url
    enlace.download = nombreArchivo
    enlace.click()
  }

  // En el celular abre el menú de compartir con la imagen y el texto; en la computadora la descarga.
  async function compartir(texto) {
    if (!actual?.blob) return
    const archivo = new File([actual.blob], nombreArchivo, { type: 'image/jpeg' })
    if (navigator.canShare?.({ files: [archivo] })) {
      try {
        await navigator.share({ files: [archivo], text: texto })
      } catch {
        // El usuario cerró el menú de compartir: no es un error.
      }
      return
    }
    descargar()
    setAviso('Tu navegador no comparte imágenes: la descargamos para que la subas a tu red social.')
  }

  return {
    formato,
    elegirFormato,
    url: actual?.url ?? null,
    cargando: !actual,
    error: actual?.error ?? null,
    aviso,
    descargar,
    compartir,
  }
}

// La imagen como data URL (para ponerla dentro del afiche, que se abre en otra ventana).
export function comoDataUrl(blob) {
  return new Promise((resolver, rechazar) => {
    const lector = new FileReader()
    lector.onload = () => resolver(lector.result)
    lector.onerror = () => rechazar(lector.error)
    lector.readAsDataURL(blob)
  })
}
