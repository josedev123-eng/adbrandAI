// Cuántas decisiones del moderador todavía no vio el dueño (el número junto a "Mis anuncios" en el menú).
// Se pide al entrar, al cambiar de pantalla y cada 2 minutos. Si falla, simplemente no se muestra el número.
import { useCallback, useEffect, useState } from 'react'
import { contarAvisos } from '../../features/misAnuncios/models/misAnunciosApi'

export const CADA_CUANTO_MS = 120_000

export function useAvisosModeracion(conSesion, pantalla) {
  const [avisos, setAvisos] = useState(0)

  useEffect(() => {
    if (!conSesion) return undefined
    let vigente = true
    const pedir = () =>
      contarAvisos()
        .then((respuesta) => {
          if (vigente) setAvisos(respuesta?.pendientes ?? 0)
        })
        .catch(() => {})
    pedir()
    const intervalo = setInterval(pedir, CADA_CUANTO_MS)
    return () => {
      vigente = false
      clearInterval(intervalo)
    }
  }, [conSesion, pantalla])

  const limpiarAvisos = useCallback(() => setAvisos(0), [])

  return { avisos: conSesion ? avisos : 0, limpiarAvisos }
}
