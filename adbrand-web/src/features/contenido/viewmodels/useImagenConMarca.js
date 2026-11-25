// ViewModel (HU 61): la imagen final del anuncio con el logo, los colores y el eslogan del kit.
// La arma Spring en el formato elegido; aquí se muestra, se descarga o se comparte (con el texto listo).
import { useEffect, useState } from 'react'
import { descargarImagenFinal } from '../models/anunciosApi'

export const FORMATOS_IMAGEN = [
  { valor: 'CUADRADO', nombre: 'Publicación', detalle: 'cuadrada' },
  { valor: 'VERTICAL', nombre: 'Historia o estado', detalle: 'vertical' },
  { valor: 'HORIZONTAL', nombre: 'Portada', detalle: 'horizontal' },
]

// "condiciones": vigencia y stock (INDECOPI) que el dueño completó; van en letra chica dentro del diseño.
export function useImagenConMarca(anuncioId, condiciones = {}) {
  const hasta = condiciones.hasta ?? ''
  const stock = condiciones.stock ?? ''
  const [formato, setFormato] = useState('CUADRADO')
  // { formato, blob, url } de la última imagen que llegó, o { formato, error }.
  const [resultado, setResultado] = useState(null)
  const [aviso, setAviso] = useState(null)

  // Mientras escribe la fecha o el stock se espera un momento antes de volver a pedir la imagen.
  const clave = `${formato}|${hasta}|${stock}`
  useEffect(() => {
    let vigente = true
    const espera = setTimeout(
      () => {
        descargarImagenFinal(anuncioId, formato, { hasta, stock })
          .then((blob) => {
            if (!vigente) return
            setResultado({ clave, formato, blob, url: URL.createObjectURL(blob) })
          })
          .catch((e) => {
            if (vigente) setResultado({ clave, formato, error: e?.message || 'No se pudo crear la imagen con tu marca.' })
          })
      },
      hasta || stock ? 500 : 0,
    )
    return () => {
      vigente = false
      clearTimeout(espera)
    }
  }, [anuncioId, formato, hasta, stock, clave])

  // La imagen anterior se libera recién cuando llega la nueva (o al salir), así nunca se ve rota.
  useEffect(() => () => resultado?.url && URL.revokeObjectURL(resultado.url), [resultado])

  // Mientras llega la versión nueva (por ejemplo, con el stock recién escrito) se sigue viendo la anterior.
  const actual = resultado?.clave === clave ? resultado : resultado?.formato === formato && resultado?.url ? resultado : null
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
