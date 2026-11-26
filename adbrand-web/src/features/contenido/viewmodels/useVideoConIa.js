// ViewModel HU 66: "Video con IA" (plan Premium, 3 al mes). La IA anima la foto del anuncio (tarda 1 a 3 minutos):
// se pide el video, se pregunta cada pocos segundos en qué va y, cuando está listo, se descarga.
import { useEffect, useRef, useState } from 'react'
import {
  consultarVideoIa,
  descargarVideoIa,
  disponibilidadVideoIa,
  iniciarVideoIa,
} from '../models/anunciosApi'

export const CADA_CUANTO_MS = 8000

export function useVideoConIa(anuncioId) {
  const [disponibilidad, setDisponibilidad] = useState(null)
  const [video, setVideo] = useState(null) // { id, estado, progreso, usados, limite }
  const [error, setError] = useState(null)
  const [pidiendo, setPidiendo] = useState(false)
  const temporizador = useRef(null)

  useEffect(() => {
    let vigente = true
    disponibilidadVideoIa()
      .then((d) => vigente && setDisponibilidad(d))
      .catch(() => vigente && setDisponibilidad(null))
    return () => {
      vigente = false
      clearTimeout(temporizador.current)
    }
  }, [])

  async function guardar(blob) {
    const url = URL.createObjectURL(blob)
    const enlace = document.createElement('a')
    enlace.href = url
    enlace.download = `anuncio-ia-${anuncioId}.mp4`
    enlace.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  }

  async function seguir(estado) {
    setVideo(estado)
    setDisponibilidad((d) => (d ? { ...d, usados: estado.usados, disponible: estado.usados < estado.limite } : d))
    if (estado.estado === 'LISTO') {
      try {
        await guardar(await descargarVideoIa(estado.id))
      } catch (e) {
        setError(e?.message || 'No se pudo descargar el video.')
      }
      return
    }
    if (estado.estado === 'FALLIDO') {
      setError(estado.error || 'La IA no pudo crear el video. No se descontó de tus 3 del mes.')
      return
    }
    temporizador.current = setTimeout(async () => {
      try {
        await seguir(await consultarVideoIa(estado.id))
      } catch (e) {
        setError(e?.message || 'Perdimos la conexión mientras se creaba tu video.')
      }
    }, CADA_CUANTO_MS)
  }

  async function crear() {
    setError(null)
    setPidiendo(true)
    try {
      await seguir(await iniciarVideoIa(anuncioId))
    } catch (e) {
      setError(e?.message || 'No se pudo pedir el video con IA.')
    } finally {
      setPidiendo(false)
    }
  }

  const creando = pidiendo || video?.estado === 'CREANDO'
  return { disponibilidad, video, creando, error, crear }
}
