// ViewModel: en qué paso va el tutorial y dónde está en pantalla el elemento que se resalta.
import { useCallback, useEffect, useState } from 'react'
import { PASOS_TUTORIAL } from '../models/tutorialApi'

const MARGEN = 6

// Rectángulo del elemento a resaltar, con un pequeño margen. null si no existe o no se ve (la tarjeta va al centro).
export function medirObjetivo(objetivo) {
  if (!objetivo || typeof document === 'undefined') return null
  const elemento = document.querySelector(`[data-tutorial="${objetivo}"]`)
  const caja = elemento?.getBoundingClientRect()
  if (!caja || caja.width === 0 || caja.height === 0) return null
  return {
    top: caja.top - MARGEN,
    left: caja.left - MARGEN,
    width: caja.width + MARGEN * 2,
    height: caja.height + MARGEN * 2,
  }
}

export function useTutorial({ alTerminar, alSaltar, pasos = PASOS_TUTORIAL }) {
  const [indice, setIndice] = useState(0)
  // Sube cada vez que la ventana cambia de tamaño o se desplaza, para volver a medir.
  const [, setMedicion] = useState(0)
  const paso = pasos[indice]
  const esUltimo = indice === pasos.length - 1

  // El menú ya está en pantalla cuando se dibuja el tutorial, así que se mide al dibujar cada paso.
  const hueco = medirObjetivo(paso.objetivo)

  useEffect(() => {
    const elemento = paso.objetivo && document.querySelector(`[data-tutorial="${paso.objetivo}"]`)
    elemento?.scrollIntoView?.({ block: 'nearest', inline: 'nearest' })
    const remedir = () => setMedicion((actual) => actual + 1)
    window.addEventListener('resize', remedir)
    window.addEventListener('scroll', remedir, true)
    return () => {
      window.removeEventListener('resize', remedir)
      window.removeEventListener('scroll', remedir, true)
    }
  }, [paso.objetivo])

  // Si quiere terminar a mitad del recorrido, primero se le pregunta: continuar o terminar.
  const [confirmando, setConfirmando] = useState(false)

  const siguiente = useCallback(() => {
    if (esUltimo) alTerminar()
    else setIndice((actual) => actual + 1)
  }, [esUltimo, alTerminar])

  const anterior = useCallback(() => setIndice((actual) => Math.max(0, actual - 1)), [])

  // En el último paso ya no hace falta preguntar: se cierra y se queda en la pantalla actual.
  const pedirTerminar = useCallback(() => {
    if (esUltimo) alSaltar()
    else setConfirmando(true)
  }, [esUltimo, alSaltar])

  const continuar = useCallback(() => setConfirmando(false), [])

  return {
    paso,
    indice,
    total: pasos.length,
    esUltimo,
    hueco,
    confirmando,
    siguiente,
    anterior,
    pedirTerminar,
    continuar,
    terminar: alSaltar,
  }
}
