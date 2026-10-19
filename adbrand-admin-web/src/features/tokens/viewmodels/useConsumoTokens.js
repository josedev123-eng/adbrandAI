// ViewModel: consumo de tokens de IA por empresa en el mes actual (HU-17).
// Criterio 2: se vuelve a pedir solo cada 30 segundos, así cada generación nueva aparece sin volver a entrar.
import { useCallback, useEffect, useRef, useState } from 'react'
import { obtenerConsumoActual } from '../models/tokensApi'

export const CADA_CUANTO_MS = 30_000
const ERROR_CARGA = 'No se pudo cargar el consumo de IA. Revisa que el servidor de administración esté encendido.'

export function useConsumoTokens() {
  const [consumo, setConsumo] = useState(null)
  const [error, setError] = useState(null)
  const [actualizando, setActualizando] = useState(false)
  // Evita guardar una respuesta que llega después de salir de la pantalla.
  const vigente = useRef(true)

  const pedir = useCallback(
    () =>
      obtenerConsumoActual()
        .then((datos) => {
          if (!vigente.current) return
          setConsumo(datos)
          setError(null)
        })
        .catch(() => {
          if (vigente.current) setError(ERROR_CARGA)
        })
        .finally(() => {
          if (vigente.current) setActualizando(false)
        }),
    [],
  )

  useEffect(() => {
    vigente.current = true
    pedir()
    const intervalo = setInterval(pedir, CADA_CUANTO_MS)
    // Al salir de la pantalla se deja de preguntar a Django.
    return () => {
      vigente.current = false
      clearInterval(intervalo)
    }
  }, [pedir])

  function actualizar() {
    setActualizando(true)
    return pedir()
  }

  return { consumo, cargando: consumo === null && !error, error, actualizando, actualizar }
}
